package vn.edu.cnpm.projectsupport.reporting.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.reporting.dto.CommitQualityResponse;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;

@Service
@Transactional(readOnly = true)
public class CommitQualityService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    public CommitQualityService(JdbcTemplate jdbc, CurrentUserService currentUserService) {
        this.jdbc = jdbc; this.currentUserService = currentUserService;
    }

    public List<CommitQualityResponse> report(Long projectId, Long requestedMemberId) {
        User current = currentUserService.findCurrentUser().orElseThrow(() -> new AccessDeniedException("Chưa xác thực"));
        Long memberId = current.getRole().getCode() == RoleCode.TEAM_MEMBER ? current.getId() : requestedMemberId;
        String sql = "SELECT u.id member_id,u.username,COUNT(DISTINCT c.id) commits,"
                + "COALESCE(SUM(c.additions),0) additions,COALESCE(SUM(c.deletions),0) deletions,"
                + "COALESCE(SUM(c.files_changed),0) files_changed,COALESCE(SUM(CASE WHEN c.is_reverted=TRUE THEN 1 ELSE 0 END),0) reverted,"
                + "COUNT(DISTINCT CASE WHEN cr.status='SUCCESS' THEN cr.id END) checks_ok,"
                + "COUNT(DISTINCT CASE WHEN cr.status IN ('FAILURE','CANCELLED') THEN cr.id END) checks_failed "
                + "FROM users u JOIN user_external_accounts ea ON ea.user_id=u.id AND ea.provider='GITHUB' "
                + "JOIN github_commits c ON c.author_external_account_id=ea.id "
                + "JOIN github_repositories r ON r.id=c.repository_id AND r.project_id=? "
                + "LEFT JOIN github_check_runs cr ON cr.repository_id=r.id AND cr.commit_sha=c.sha "
                + (memberId == null ? "" : "WHERE u.id=? ") + "GROUP BY u.id,u.username ORDER BY u.username";
        Object[] args = memberId == null ? new Object[]{projectId} : new Object[]{projectId, memberId};
        return jdbc.query(sql, (rs, n) -> {
            long commits=rs.getLong("commits"), reverted=rs.getLong("reverted");
            long ok=rs.getLong("checks_ok"), failed=rs.getLong("checks_failed"), checks=ok+failed;
            double success=checks==0?0.0:round(ok*100.0/checks);
            double revertPenalty=commits==0?0.0:reverted*30.0/commits;
            double checkPenalty=checks==0?20.0:failed*50.0/checks;
            double score=round(Math.max(0.0,100.0-revertPenalty-checkPenalty));
            String rating=score>=90?"EXCELLENT":score>=75?"GOOD":score>=60?"ACCEPTABLE":"NEEDS_IMPROVEMENT";
            return new CommitQualityResponse(rs.getLong("member_id"),rs.getString("username"),commits,
                    rs.getLong("additions"),rs.getLong("deletions"),rs.getLong("files_changed"),reverted,
                    ok,failed,success,score,rating);
        }, args);
    }
    private static double round(double v){return Math.round(v*100.0)/100.0;}
}
