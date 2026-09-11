package vn.edu.cnpm.projectsupport.reporting;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.reporting.dto.*;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    @Value("${reporting.freshness-threshold:24h}")
    private Duration freshnessThreshold;

    public ReportSummaryResponse getSummaryReport(Long projectId, ReportFilterRequest filter, User currentUser) {
        Instant asOf = Instant.now();

        // 1. Kiểm tra Project tồn tại
        Number projectCount = (Number) em.createNativeQuery(
                "SELECT COUNT(1) FROM projects WHERE id = :pid")
                .setParameter("pid", projectId)
                .getSingleResult();
        if (projectCount.longValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND");
        }

        // 2. Phân quyền & Ép scope TEAM_MEMBER
        Long targetMemberId = filter.memberId();
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().toString();
            // Nếu là TEAM_MEMBER -> bắt buộc ép về chính mình
            if (roleName.contains("MEMBER") && !roleName.contains("LEADER")) {
                targetMemberId = currentUser.getId();
            }
        }

        // 3. Kiểm tra Sprint thuộc Project (nếu có filter sprintId)
        if (filter.sprintId() != null) {
            Number sprintCount = (Number) em.createNativeQuery(
                    "SELECT COUNT(1) FROM sprints WHERE id = :sid AND project_id = :pid")
                    .setParameter("sid", filter.sprintId())
                    .setParameter("pid", projectId)
                    .getSingleResult();
            if (sprintCount.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SPRINT_NOT_FOUND");
            }
        }

        // 4. Kiểm tra Member active trong Project (nếu có filter memberId)
        if (targetMemberId != null) {
            Number memberCount = (Number) em.createNativeQuery("""
                SELECT COUNT(1) 
                FROM group_members gm 
                JOIN projects p ON gm.group_id = p.group_id 
                WHERE p.id = :pid AND gm.user_id = :uid AND gm.active = true
            """)
            .setParameter("pid", projectId)
            .setParameter("uid", targetMemberId)
            .getSingleResult();

            if (memberCount.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND");
            }
        }

        // 5. Tính toán Task Metrics (đủ 6 status, total, completed, overdue)
        TaskMetricsResponse taskMetrics = calculateTaskMetrics(
                projectId, filter.sprintId(), targetMemberId, filter.from(), filter.to(), asOf);

        // 6. Tính toán Đóng góp thành viên (commits, PRs, linked tasks)
        List<MemberContributionResponse> memberContributions = calculateMemberContributions(
                projectId, filter.sprintId(), targetMemberId, filter.from(), filter.to());

        // 7. Nguồn dữ liệu (Sources Freshness) & Cảnh báo (Warnings)
        List<ReportSourceFreshnessResponse> sources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        sources.add(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null));
        sources.add(evaluateSourceFreshness(projectId, "JIRA", filter.to(), asOf, warnings, "JIRA_NOT_SYNCED", "JIRA_SYNC_FAILED"));
        sources.add(evaluateSourceFreshness(projectId, "GITHUB", filter.to(), asOf, warnings, "GITHUB_NOT_SYNCED", "GITHUB_SYNC_FAILED"));

        // Kiểm tra mapping tài khoản GitHub
        Number githubLinkedCount = (Number) em.createNativeQuery("""
            SELECT COUNT(uea.id)
            FROM user_external_accounts uea
            JOIN group_members gm ON uea.user_id = gm.user_id
            JOIN projects p ON gm.group_id = p.group_id
            WHERE p.id = :pid AND uea.provider = 'GITHUB'
        """)
        .setParameter("pid", projectId)
        .getSingleResult();

        if (githubLinkedCount.longValue() == 0) {
            warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
        }

        // 8. Đánh giá ReportDataStatus
        ReportDataStatus dataStatus = resolveDataStatus(sources);

        return new ReportSummaryResponse(
                projectId,
                filter.sprintId(),
                targetMemberId,
                filter.from(),
                filter.to(),
                asOf,
                taskMetrics,
                memberContributions,
                dataStatus,
                sources,
                warnings
        );
    }

    private TaskMetricsResponse calculateTaskMetrics(
            Long projectId, Long sprintId, Long memberId, Instant from, Instant to, Instant asOf) {

        StringBuilder where = new StringBuilder(" WHERE t.project_id = :projectId ");
        if (sprintId != null) where.append(" AND t.sprint_id = :sprintId ");
        if (memberId != null) where.append(" AND t.assignee_user_id = :memberId ");
        if (from != null) where.append(" AND t.created_at >= :from ");
        if (to != null) where.append(" AND t.created_at < :to ");

        // 1. Phân nhóm status
        String statusSql = "SELECT t.status, COUNT(DISTINCT t.id) FROM tasks t " + where + " GROUP BY t.status";
        Query statusQ = em.createNativeQuery(statusSql);
        bindFilterParams(statusQ, projectId, sprintId, memberId, from, to);

        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, 0L);
        }

        long total = 0;
        long completed = 0;
        for (Object item : statusQ.getResultList()) {
            Object[] row = (Object[]) item;
            try {
                TaskStatus st = TaskStatus.valueOf((String) row[0]);
                long count = ((Number) row[1]).longValue();
                byStatus.put(st, count);
                total += count;
                if (st == TaskStatus.DONE) {
                    completed = count;
                }
            } catch (Exception ignored) {}
        }

        // 2. Overdue tasks: deadline < asOf AND status NOT IN ('DONE', 'CANCELLED')
        String overdueSql = "SELECT COUNT(DISTINCT t.id) FROM tasks t " + where
                + " AND t.deadline IS NOT NULL AND t.deadline < :asOf AND t.status NOT IN ('DONE', 'CANCELLED')";
        Query overdueQ = em.createNativeQuery(overdueSql);
        bindFilterParams(overdueQ, projectId, sprintId, memberId, from, to);
        overdueQ.setParameter("asOf", asOf);

        long overdue = ((Number) overdueQ.getSingleResult()).longValue();

        return new TaskMetricsResponse(total, completed, overdue, byStatus);
    }

    @SuppressWarnings("unchecked")
    private List<MemberContributionResponse> calculateMemberContributions(
            Long projectId, Long sprintId, Long memberId, Instant from, Instant to) {

        StringBuilder userSql = new StringBuilder("""
            SELECT u.id, u.username, u.full_name
            FROM users u
            JOIN group_members gm ON u.id = gm.user_id
            JOIN projects p ON gm.group_id = p.group_id
            WHERE p.id = :projectId AND gm.active = true
        """);
        if (memberId != null) {
            userSql.append(" AND u.id = :memberId ");
        }

        Query userQ = em.createNativeQuery(userSql.toString());
        userQ.setParameter("projectId", projectId);
        if (memberId != null) userQ.setParameter("memberId", memberId);

        List<Object[]> users = userQ.getResultList();
        List<MemberContributionResponse> result = new ArrayList<>();

        for (Object[] u : users) {
            Long uid = ((Number) u[0]).longValue();
            String username = (String) u[1];
            String fullName = (String) u[2];

            // Commits
            StringBuilder commitSql = new StringBuilder("""
                SELECT COUNT(DISTINCT c.id)
                FROM github_commits c
                JOIN user_external_accounts uea ON c.account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON c.repository_id = r.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (from != null) commitSql.append(" AND c.committed_at >= :from ");
            if (to != null) commitSql.append(" AND c.committed_at < :to ");
            if (sprintId != null) {
                commitSql.append(" AND EXISTS (SELECT 1 FROM task_commit_links tcl JOIN tasks t ON tcl.task_id = t.id WHERE tcl.commit_id = c.id AND t.sprint_id = :sprintId) ");
            }
            Query cq = em.createNativeQuery(commitSql.toString());
            cq.setParameter("projectId", projectId);
            cq.setParameter("uid", uid);
            if (sprintId != null) cq.setParameter("sprintId", sprintId);
            if (from != null) cq.setParameter("from", from);
            if (to != null) cq.setParameter("to", to);
            long commits = ((Number) cq.getSingleResult()).longValue();

            // Pull Requests
            StringBuilder prSql = new StringBuilder("""
                SELECT COUNT(DISTINCT pr.id)
                FROM github_pull_requests pr
                JOIN user_external_accounts uea ON pr.account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON pr.repository_id = r.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (from != null) prSql.append(" AND pr.created_at >= :from ");
            if (to != null) prSql.append(" AND pr.created_at < :to ");
            if (sprintId != null) {
                prSql.append(" AND EXISTS (SELECT 1 FROM task_pull_request_links tprl JOIN tasks t ON tprl.pull_request_id = pr.id WHERE t.sprint_id = :sprintId) ");
            }
            Query prq = em.createNativeQuery(prSql.toString());
            prq.setParameter("projectId", projectId);
            prq.setParameter("uid", uid);
            if (sprintId != null) prq.setParameter("sprintId", sprintId);
            if (from != null) prq.setParameter("from", from);
            if (to != null) prq.setParameter("to", to);
            long pullRequests = ((Number) prq.getSingleResult()).longValue();

            // Linked Tasks: Hợp giữa Task-Commit và Task-PR
            String linkedSql = """
                SELECT COUNT(DISTINCT lk.task_id) FROM (
                    SELECT tcl.task_id
                    FROM task_commit_links tcl
                    JOIN github_commits c ON tcl.commit_id = c.id
                    JOIN user_external_accounts uea ON c.account_id = uea.id AND uea.provider = 'GITHUB'
                    JOIN github_repositories r ON c.repository_id = r.id
                    WHERE r.project_id = :projectId AND uea.user_id = :uid
                    UNION
                    SELECT tprl.task_id
                    FROM task_pull_request_links tprl
                    JOIN github_pull_requests pr ON tprl.pull_request_id = pr.id
                    JOIN user_external_accounts uea ON pr.account_id = uea.id AND uea.provider = 'GITHUB'
                    JOIN github_repositories r ON pr.repository_id = r.id
                    WHERE r.project_id = :projectId AND uea.user_id = :uid
                ) lk
            """;
            Query lq = em.createNativeQuery(linkedSql);
            lq.setParameter("projectId", projectId);
            lq.setParameter("uid", uid);
            long linkedTasks = ((Number) lq.getSingleResult()).longValue();

            result.add(new MemberContributionResponse(uid, username, fullName, commits, pullRequests, linkedTasks));
        }

        return result;
    }

    private ReportSourceFreshnessResponse evaluateSourceFreshness(
            Long projectId, String type, Instant to, Instant asOf,
            List<String> warnings, String notSyncedWarning, String failedWarning) {

        ReportSource source = ReportSource.valueOf(type);

        // Lấy lần sync thành công gần nhất
        Query successQ = em.createNativeQuery(
                "SELECT MAX(synced_at) FROM sync_logs WHERE project_id = :pid AND integration_type = :type AND status = 'SUCCESS'")
                .setParameter("pid", projectId)
                .setParameter("type", type);
        Object successRes = null;
        try {
            successRes = successQ.getSingleResult();
        } catch (Exception ignored) {}

        Instant lastSuccess = successRes != null ? ((Timestamp) successRes).toInstant() : null;

        // Lấy trạng thái lần sync mới nhất
        Query latestQ = em.createNativeQuery(
                "SELECT status, synced_at FROM sync_logs WHERE project_id = :pid AND integration_type = :type ORDER BY synced_at DESC LIMIT 1")
                .setParameter("pid", projectId)
                .setParameter("type", type);
        List<?> latestList = latestQ.getResultList();

        if (lastSuccess == null) {
            warnings.add(notSyncedWarning);
            return new ReportSourceFreshnessResponse(source, ReportSourceStatus.NOT_SYNCED, null);
        }

        if (!latestList.isEmpty()) {
            Object[] row = (Object[]) latestList.get(0);
            String latestStatus = (String) row[0];
            Instant latestTime = ((Timestamp) row[1]).toInstant();
            if ("FAILED".equals(latestStatus) && latestTime.isAfter(lastSuccess)) {
                warnings.add(failedWarning);
                return new ReportSourceFreshnessResponse(source, ReportSourceStatus.SYNC_FAILED, lastSuccess);
            }
        }

        if (to != null) {
            if (!lastSuccess.isBefore(to)) {
                return new ReportSourceFreshnessResponse(source, ReportSourceStatus.CURRENT, lastSuccess);
            }
        } else {
            if (Duration.between(lastSuccess, asOf).compareTo(freshnessThreshold) <= 0) {
                return new ReportSourceFreshnessResponse(source, ReportSourceStatus.CURRENT, lastSuccess);
            }
        }

        return new ReportSourceFreshnessResponse(source, ReportSourceStatus.STALE, lastSuccess);
    }

    private ReportDataStatus resolveDataStatus(List<ReportSourceFreshnessResponse> sources) {
        if (sources.stream().allMatch(s -> s.status() == ReportSourceStatus.CURRENT)) {
            return ReportDataStatus.COMPLETE;
        }
        boolean hasRemoteData = sources.stream()
                .filter(s -> s.source() != ReportSource.LOCAL_TASK)
                .anyMatch(s -> s.status() != ReportSourceStatus.NOT_SYNCED);

        return hasRemoteData ? ReportDataStatus.PARTIAL : ReportDataStatus.NOT_SYNCED;
    }

    private void bindFilterParams(Query q, Long projectId, Long sprintId, Long memberId, Instant from, Instant to) {
        q.setParameter("projectId", projectId);
        if (sprintId != null) q.setParameter("sprintId", sprintId);
        if (memberId != null) q.setParameter("memberId", memberId);
        if (from != null) q.setParameter("from", from);
        if (to != null) q.setParameter("to", to);
    }
}