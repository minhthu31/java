package vn.edu.cnpm.projectsupport.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.reporting.repository.ReportingRepository;

@SpringBootTest
@ActiveProfiles("test")
class ReportingRepositoryIntegrationTest {

    private static final long GITHUB_REPOSITORY_ID = 991234567L;
    private static final String EXTERNAL_USER_ID = "991234567";
    private static final String SHA = "991234567890abcdef991234567890abcdef991234567890abcdef9912345678";
    private static final int PR_OPEN = 991;
    private static final int PR_CLOSED = 992;
    private static final int PR_MERGED = 993;

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ReportingRepository reportingRepository;
    @Autowired ProjectRepository projectRepository;

    private long projectId;
    private long memberId;
    private long taskId;

    @BeforeEach
    void setUp() {
        projectId = jdbcTemplate.queryForObject("SELECT id FROM projects WHERE name = 'CNPM Project Management Tool' LIMIT 1",Long.class);
        memberId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'member.test'", Long.class);
        taskId = jdbcTemplate.queryForObject("SELECT id FROM tasks WHERE project_id = ? LIMIT 1", Long.class, projectId);
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void queriesDistinctActivitiesAndUsesRemotePrCreationTime() {
        long accountId = insertAccount();
        insertRepository();
        Instant from = Instant.parse("2026-09-10T10:00:00Z");
        Instant boundary = Instant.parse("2026-09-11T00:00:00Z");
        Instant after = Instant.parse("2026-09-11T00:00:00Z");
        Instant asOf = Instant.parse("2026-09-12T00:00:00Z");

        insertCommit(accountId, from.plusSeconds(1));
        insertPr(accountId, PR_OPEN, "OPEN", from.plusSeconds(2));
        insertPr(accountId, PR_CLOSED, "CLOSED", boundary.minusSeconds(1));
        insertPr(accountId, PR_MERGED, "MERGED", after);

        // Local created_at is deliberately outside the report range.
        // The query must still count the PR because remote_created_at is in range.
        jdbcTemplate.update(
                "UPDATE github_pull_requests SET created_at = ? WHERE number = ?",
                asOf.minusSeconds(1), PR_OPEN);

        long commits = reportingRepository.countCommits(projectId, null, memberId, from, boundary, asOf);
        long pullRequests = reportingRepository.countPullRequests(projectId, null, memberId, from, boundary, asOf);
        long open = reportingRepository.countPullRequestsByState(projectId, null, memberId, "OPEN", from, boundary, asOf);
        long closed = reportingRepository.countPullRequestsByState(projectId, null, memberId, "CLOSED", from, boundary, asOf);
        long merged = reportingRepository.countPullRequestsByState(projectId, null, memberId, "MERGED", from, boundary, asOf);

        assertThat(commits).isEqualTo(1);
        assertThat(pullRequests).isEqualTo(2);
        assertThat(open).isEqualTo(1);
        assertThat(closed).isEqualTo(1);
        assertThat(merged).isZero();

        assertThatThrownBy(() -> insertCommit(accountId, from.plusSeconds(1))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPr(accountId, PR_OPEN, "OPEN", from.plusSeconds(2))).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(reportingRepository.countCommits(projectId, null, memberId, from, boundary, asOf)).isEqualTo(1);
        assertThat(reportingRepository.countPullRequests(projectId, null, memberId, from, boundary, asOf)).isEqualTo(2);

    }

    @Test
    void activeMembersIncludeLeaderStoredOnlyOnStudentGroup() {
        List<ProjectRepository.ActiveMemberProjection> members = projectRepository.findActiveMembers(projectId);

        long leaderId = jdbcTemplate.queryForObject("SELECT leader_user_id FROM student_groups g JOIN projects p ON p.group_id = g.id WHERE p.id = ?", Long.class, projectId);

        assertThat(members).extracting(ProjectRepository.ActiveMemberProjection::getId).contains(leaderId, memberId);
    }

    private long insertAccount() {
        jdbcTemplate.update("""
                INSERT INTO user_external_accounts
                    (user_id, provider, external_user_id, external_login)
                VALUES (?, 'GITHUB', ?, 'report-member')
                """, memberId, EXTERNAL_USER_ID);
        return jdbcTemplate.queryForObject("SELECT id FROM user_external_accounts WHERE external_user_id = ?", Long.class, EXTERNAL_USER_ID);
    }

    private void insertRepository() {
        jdbcTemplate.update("""
                INSERT INTO github_repositories
                    (project_id, github_repository_id, full_name, default_branch, html_url,
                     node_id, name, owner_github_user_id, owner_login, private_repository, archived)
                VALUES (?, ?, 'report/test', 'main', 'https://github.com/report/test',
                        'node-report', 'test', 991234567, 'report', FALSE, FALSE)
                """, projectId, GITHUB_REPOSITORY_ID);
    }

    private void insertCommit(long accountId, Instant committedAt) {
        jdbcTemplate.update("""
                INSERT INTO github_commits
                    (repository_id, author_external_account_id, author_github_user_id, author_login,
                     sha, message, committed_at, html_url, additions, deletions, is_reverted)
                VALUES ((SELECT id FROM github_repositories WHERE github_repository_id = ?),
                        ?, 991234567, 'report-member', ?, 'test commit', ?, 'https://github.com/report/test/commit/x',
                        1, 1, FALSE)
                """, GITHUB_REPOSITORY_ID, accountId, SHA, committedAt);
    }

    private void insertPr(long accountId, int number, String state, Instant remoteCreatedAt) {
        jdbcTemplate.update("""
                INSERT INTO github_pull_requests
                    (repository_id, github_pull_request_id, author_external_account_id,
                     author_github_user_id, author_login, number, title, body, head_ref, head_sha,
                     base_ref, state, draft, additions, deletions, html_url, remote_created_at)
                VALUES ((SELECT id FROM github_repositories WHERE github_repository_id = ?),
                        ?, ?, 991234567, 'report-member', ?, 'test pr', NULL, 'feature/test', 'headsha',
                        'main', ?, FALSE, 1, 1, ?, ?)
                """, GITHUB_REPOSITORY_ID, (long) number, accountId, number, state,
                "https://github.com/report/test/pull/" + number, remoteCreatedAt);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM task_commit_links WHERE commit_id IN " +
                "(SELECT id FROM github_commits WHERE repository_id IN " +
                "(SELECT id FROM github_repositories WHERE github_repository_id = ?))", GITHUB_REPOSITORY_ID);
        jdbcTemplate.update("DELETE FROM task_pr_links WHERE pull_request_id IN " +
                "(SELECT id FROM github_pull_requests WHERE repository_id IN " +
                "(SELECT id FROM github_repositories WHERE github_repository_id = ?))", GITHUB_REPOSITORY_ID);
        jdbcTemplate.update("DELETE FROM github_pull_requests WHERE repository_id IN " +
                "(SELECT id FROM github_repositories WHERE github_repository_id = ?)", GITHUB_REPOSITORY_ID);
        jdbcTemplate.update("DELETE FROM github_commits WHERE repository_id IN " +
                "(SELECT id FROM github_repositories WHERE github_repository_id = ?)", GITHUB_REPOSITORY_ID);
        jdbcTemplate.update("DELETE FROM github_repositories WHERE github_repository_id = ?", GITHUB_REPOSITORY_ID);
        jdbcTemplate.update("DELETE FROM user_external_accounts WHERE external_user_id = ?", EXTERNAL_USER_ID);
    }
}
