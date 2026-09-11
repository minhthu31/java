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
import vn.edu.cnpm.projectsupport.reporting.dto.MemberContributionResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSource;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceFreshnessResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.TaskMetricsResponse;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    @Value("${reporting.freshness-threshold:24h}")
    private Duration freshnessThreshold;

    public ReportSummaryResponse getSummaryReport(Long projectId, ReportFilterRequest filter, String currentUsername) {
        Instant asOf = Instant.now();

        if (currentUsername == null || currentUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        // 1. Join bảng roles qua role_id để lấy thông tin và quyền
        Query userQuery = em.createNativeQuery("""
            SELECT u.id, r.code
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE u.username = :uname
        """);
        userQuery.setParameter("uname", currentUsername);
        List<?> userList = userQuery.getResultList();
        if (userList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        Object[] userRow = (Object[]) userList.get(0);
        Long currentUserId = ((Number) userRow[0]).longValue();
        String roleCode = (String) userRow[1];

        // 2. Lấy group_id của Project
        Query projectQuery = em.createNativeQuery("SELECT id, group_id FROM projects WHERE id = :pid");
        projectQuery.setParameter("pid", projectId);
        List<?> projectList = projectQuery.getResultList();
        if (projectList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        Object[] projectRow = (Object[]) projectList.get(0);
        Long groupId = projectRow[1] != null ? ((Number) projectRow[1]).longValue() : null;

        // 3. Kiểm tra RBAC theo student_groups, group_lecturers, student_groups.leader_user_id
        if ("LECTURER".equals(roleCode)) {
            Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(1)
                FROM group_lecturers
                WHERE group_id = :gid AND lecturer_user_id = :uid
            """)
            .setParameter("gid", groupId)
            .setParameter("uid", currentUserId)
            .getSingleResult();

            if (count.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REPORT_ACCESS_DENIED");
            }
        } else if ("TEAM_LEADER".equals(roleCode)) {
            Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(1)
                FROM student_groups
                WHERE id = :gid AND leader_user_id = :uid
            """)
            .setParameter("gid", groupId)
            .setParameter("uid", currentUserId)
            .getSingleResult();

            if (count.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REPORT_ACCESS_DENIED");
            }
        } else if ("TEAM_MEMBER".equals(roleCode)) {
            Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(1)
                FROM group_members
                WHERE group_id = :gid AND user_id = :uid AND status = 'ACTIVE'
            """)
            .setParameter("gid", groupId)
            .setParameter("uid", currentUserId)
            .getSingleResult();

            if (count.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REPORT_ACCESS_DENIED");
            }
        } else if (!"ADMIN".equals(roleCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REPORT_ACCESS_DENIED");
        }

        // 4. Ép scope memberId về chính mình nếu là TEAM_MEMBER
        Long targetMemberId = filter.memberId();
        if ("TEAM_MEMBER".equals(roleCode)) {
            targetMemberId = currentUserId;
        }

        // 5. Kiểm tra Sprint thuộc Project
        if (filter.sprintId() != null) {
            Number sprintCount = (Number) em.createNativeQuery("""
                SELECT COUNT(1)
                FROM sprints
                WHERE id = :sid AND project_id = :pid
            """)
            .setParameter("sid", filter.sprintId())
            .setParameter("pid", projectId)
            .getSingleResult();

            if (sprintCount.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SPRINT_NOT_FOUND");
            }
        }

        // 6. Kiểm tra Member active trong Project (status = 'ACTIVE' hoặc là Leader của student_groups)
        if (targetMemberId != null) {
            Number activeMemberCount = (Number) em.createNativeQuery("""
                SELECT COUNT(1)
                FROM projects p
                JOIN student_groups g ON g.id = p.group_id
                WHERE p.id = :pid AND (
                    g.leader_user_id = :uid
                    OR EXISTS (
                        SELECT 1 FROM group_members gm
                        WHERE gm.group_id = g.id AND gm.user_id = :uid AND gm.status = 'ACTIVE'
                    )
                )
            """)
            .setParameter("pid", projectId)
            .setParameter("uid", targetMemberId)
            .getSingleResult();

            if (activeMemberCount.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND");
            }
        }

        // 7. Tính Task Metrics
        TaskMetricsResponse taskMetrics = calculateTaskMetrics(
                projectId, filter.sprintId(), targetMemberId, filter.from(), filter.to(), asOf);

        // 8. Tính Member Contributions
        List<MemberContributionResponse> memberContributions = calculateMemberContributions(
                projectId, filter.sprintId(), targetMemberId, filter.from(), filter.to());

        // 9. Nguồn dữ liệu Freshness & Cảnh báo
        List<ReportSourceFreshnessResponse> sources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        sources.add(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null));
        sources.add(evaluateSourceFreshness(projectId, "JIRA", filter.to(), asOf, warnings, "JIRA_NOT_SYNCED", "JIRA_SYNC_FAILED"));
        sources.add(evaluateSourceFreshness(projectId, "GITHUB", filter.to(), asOf, warnings, "GITHUB_NOT_SYNCED", "GITHUB_SYNC_FAILED"));

        Number githubLinkedCount = (Number) em.createNativeQuery("""
            SELECT COUNT(uea.id)
            FROM user_external_accounts uea
            JOIN group_members gm ON uea.user_id = gm.user_id
            JOIN projects p ON gm.group_id = p.group_id
            WHERE p.id = :pid AND uea.provider = 'GITHUB' AND gm.status = 'ACTIVE'
        """)
        .setParameter("pid", projectId)
        .getSingleResult();

        if (githubLinkedCount.longValue() == 0) {
            warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
        }

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
        if (sprintId != null) {
            where.append(" AND t.sprint_id = :sprintId ");
        }
        if (memberId != null) {
            where.append(" AND t.assignee_user_id = :memberId ");
        }
        if (from != null) {
            where.append(" AND t.created_at >= :from ");
        }
        if (to != null) {
            where.append(" AND t.created_at < :to ");
        }

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
            } catch (Exception ignored) {
            }
        }

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
            SELECT DISTINCT u.id, u.username, u.full_name
            FROM users u
            JOIN projects p ON p.id = :projectId
            JOIN student_groups g ON g.id = p.group_id
            LEFT JOIN group_members gm ON gm.group_id = g.id AND gm.user_id = u.id
            WHERE (gm.status = 'ACTIVE' OR g.leader_user_id = u.id)
        """);
        if (memberId != null) {
            userSql.append(" AND u.id = :memberId ");
        }

        Query userQ = em.createNativeQuery(userSql.toString());
        userQ.setParameter("projectId", projectId);
        if (memberId != null) {
            userQ.setParameter("memberId", memberId);
        }

        List<Object[]> users = userQ.getResultList();
        List<MemberContributionResponse> result = new ArrayList<>();

        for (Object[] u : users) {
            Long uid = ((Number) u[0]).longValue();
            String username = (String) u[1];
            String fullName = (String) u[2];

            // 1. Commits (dùng author_external_account_id và provider)
            StringBuilder commitSql = new StringBuilder("""
                SELECT COUNT(DISTINCT c.id)
                FROM github_commits c
                JOIN user_external_accounts uea ON c.author_external_account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON c.repository_id = r.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (from != null) {
                commitSql.append(" AND c.committed_at >= :from ");
            }
            if (to != null) {
                commitSql.append(" AND c.committed_at < :to ");
            }
            if (sprintId != null) {
                commitSql.append("""
                    AND EXISTS (
                        SELECT 1 FROM task_commit_links tcl
                        JOIN tasks t ON tcl.task_id = t.id
                        WHERE tcl.commit_id = c.id AND t.sprint_id = :sprintId
                    )
                """);
            }
            Query cq = em.createNativeQuery(commitSql.toString());
            cq.setParameter("projectId", projectId);
            cq.setParameter("uid", uid);
            if (sprintId != null) {
                cq.setParameter("sprintId", sprintId);
            }
            if (from != null) {
                cq.setParameter("from", from);
            }
            if (to != null) {
                cq.setParameter("to", to);
            }
            long commits = ((Number) cq.getSingleResult()).longValue();

            // 2. Pull Requests (dùng remote_created_at và task_pr_links)
            StringBuilder prSql = new StringBuilder("""
                SELECT COUNT(DISTINCT pr.id)
                FROM github_pull_requests pr
                JOIN user_external_accounts uea ON pr.author_external_account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON pr.repository_id = r.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (from != null) {
                prSql.append(" AND pr.remote_created_at >= :from ");
            }
            if (to != null) {
                prSql.append(" AND pr.remote_created_at < :to ");
            }
            if (sprintId != null) {
                prSql.append("""
                    AND EXISTS (
                        SELECT 1 FROM task_pr_links tprl
                        JOIN tasks t ON tprl.pull_request_id = pr.id
                        WHERE t.sprint_id = :sprintId
                    )
                """);
            }
            Query prq = em.createNativeQuery(prSql.toString());
            prq.setParameter("projectId", projectId);
            prq.setParameter("uid", uid);
            if (sprintId != null) {
                prq.setParameter("sprintId", sprintId);
            }
            if (from != null) {
                prq.setParameter("from", from);
            }
            if (to != null) {
                prq.setParameter("to", to);
            }
            long pullRequests = ((Number) prq.getSingleResult()).longValue();

            // 3. Linked Tasks: Áp dụng đầy đủ bộ lọc sprintId, from và to
            StringBuilder linkedSql = new StringBuilder("SELECT COUNT(DISTINCT lk.task_id) FROM (");

            linkedSql.append("""
                SELECT tcl.task_id AS task_id
                FROM task_commit_links tcl
                JOIN github_commits c ON tcl.commit_id = c.id
                JOIN user_external_accounts uea ON c.author_external_account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON c.repository_id = r.id
                JOIN tasks t ON tcl.task_id = t.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (sprintId != null) {
                linkedSql.append(" AND t.sprint_id = :sprintId ");
            }
            if (from != null) {
                linkedSql.append(" AND c.committed_at >= :from ");
            }
            if (to != null) {
                linkedSql.append(" AND c.committed_at < :to ");
            }

            linkedSql.append(" UNION ");

            linkedSql.append("""
                SELECT tprl.task_id AS task_id
                FROM task_pr_links tprl
                JOIN github_pull_requests pr ON tprl.pull_request_id = pr.id
                JOIN user_external_accounts uea ON pr.author_external_account_id = uea.id AND uea.provider = 'GITHUB'
                JOIN github_repositories r ON pr.repository_id = r.id
                JOIN tasks t ON tprl.task_id = t.id
                WHERE r.project_id = :projectId AND uea.user_id = :uid
            """);
            if (sprintId != null) {
                linkedSql.append(" AND t.sprint_id = :sprintId ");
            }
            if (from != null) {
                linkedSql.append(" AND pr.remote_created_at >= :from ");
            }
            if (to != null) {
                linkedSql.append(" AND pr.remote_created_at < :to ");
            }

            linkedSql.append(") lk");

            Query lq = em.createNativeQuery(linkedSql.toString());
            lq.setParameter("projectId", projectId);
            lq.setParameter("uid", uid);
            if (sprintId != null) {
                lq.setParameter("sprintId", sprintId);
            }
            if (from != null) {
                lq.setParameter("from", from);
            }
            if (to != null) {
                lq.setParameter("to", to);
            }
            long linkedTasks = ((Number) lq.getSingleResult()).longValue();

            result.add(new MemberContributionResponse(uid, username, fullName, commits, pullRequests, linkedTasks));
        }

        return result;
    }

    private ReportSourceFreshnessResponse evaluateSourceFreshness(
            Long projectId, String provider, Instant to, Instant asOf,
            List<String> warnings, String notSyncedWarning, String failedWarning) {

        ReportSource source = ReportSource.valueOf(provider);

        Query successQ = em.createNativeQuery("""
            SELECT MAX(completed_at)
            FROM sync_logs
            WHERE project_id = :pid AND provider = :provider AND status = 'SUCCESS'
        """)
        .setParameter("pid", projectId)
        .setParameter("provider", provider);

        Object successRes = null;
        try {
            successRes = successQ.getSingleResult();
        } catch (Exception ignored) {
        }

        Instant lastSuccess = successRes != null ? ((Timestamp) successRes).toInstant() : null;

        Query latestQ = em.createNativeQuery("""
            SELECT status, completed_at, started_at
            FROM sync_logs
            WHERE project_id = :pid AND provider = :provider
            ORDER BY id DESC LIMIT 1
        """)
        .setParameter("pid", projectId)
        .setParameter("provider", provider);

        List<?> latestList = latestQ.getResultList();

        if (lastSuccess == null) {
            warnings.add(notSyncedWarning);
            return new ReportSourceFreshnessResponse(source, ReportSourceStatus.NOT_SYNCED, null);
        }

        if (!latestList.isEmpty()) {
            Object[] row = (Object[]) latestList.get(0);
            String latestStatus = (String) row[0];
            Timestamp latestTs = row[1] != null ? (Timestamp) row[1] : (Timestamp) row[2];
            Instant latestTime = latestTs != null ? latestTs.toInstant() : null;
            if ("FAILED".equals(latestStatus) && latestTime != null && latestTime.isAfter(lastSuccess)) {
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
        if (sprintId != null) {
            q.setParameter("sprintId", sprintId);
        }
        if (memberId != null) {
            q.setParameter("memberId", memberId);
        }
        if (from != null) {
            q.setParameter("from", from);
        }
        if (to != null) {
            q.setParameter("to", to);
        }
    }
}