UPDATE projects
SET jira_site_url = 'https://demo.atlassian.net',
    jira_project_id = '10001',
    jira_project_key = 'CNPM',
    start_date = '2026-08-01',
    end_date = '2026-09-30',
    jira_last_synced_at = '2026-09-13 01:00:00'
WHERE name = 'CNPM Project Management Tool';

INSERT INTO requirements (
    project_id, jira_issue_key, title, description, actor, priority,
    precondition, main_flow, alternative_flow, exception_flow,
    postcondition, status
)
SELECT p.id, 'CNPM-201', 'Đăng nhập theo bốn vai trò',
       'Người dùng đăng nhập và nhận đúng quyền theo vai trò.',
       'Demo user', 'HIGH', 'Tài khoản đang ACTIVE.',
       'Nhập username/email và mật khẩu, hệ thống xác thực rồi cấp JWT.',
       'Cho phép dùng username hoặc email.',
       'Sai thông tin thì từ chối đăng nhập.',
       'Người dùng vào được màn hình phù hợp với vai trò.', 'DONE'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM requirements r
                  WHERE r.project_id = p.id AND r.jira_issue_key = 'CNPM-201');

INSERT INTO requirements (
    project_id, jira_issue_key, title, description, actor, priority,
    precondition, main_flow, alternative_flow, exception_flow,
    postcondition, status
)
SELECT p.id, 'CNPM-202', 'Xem báo cáo tiến độ từ Jira và GitHub',
       'Hiển thị task, commit và Pull Request theo thành viên và khoảng thời gian.',
       'Demo user', 'HIGH', 'Project có dữ liệu đồng bộ.',
       'Chọn project và khoảng thời gian, sau đó xem thống kê.',
       'Có thể lọc theo thành viên.',
       'Nếu nguồn chưa đồng bộ thì hiển thị trạng thái tương ứng.',
       'Báo cáo có số liệu task, commit và Pull Request.', 'DONE'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM requirements r
                  WHERE r.project_id = p.id AND r.jira_issue_key = 'CNPM-202');

INSERT INTO features (project_id, jira_epic_key, name, description)
SELECT p.id, 'CNPM-EPIC-2', 'Authentication and Reporting',
       'Demo feature cho đăng nhập và báo cáo tích hợp.'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM features f
                  WHERE f.project_id = p.id AND f.jira_epic_key = 'CNPM-EPIC-2');

INSERT INTO tasks (
    project_id, requirement_id, feature_id, sprint_id, assignee_user_id,
    title, description, acceptance_criteria, issue_type, classification,
    priority, deadline, status, sync_status, idempotency_key
)
SELECT p.id, r.id, f.id, s.id, u.id,
       'Demo báo cáo tiến độ Jira và GitHub',
       'Task mẫu phục vụ trình diễn báo cáo cuối kỳ.',
       'Báo cáo hiển thị task, commit và Pull Request theo khoảng thời gian.',
       'TASK', 'FEATURE_RELATED', 'HIGH', '2026-09-20 23:59:59',
       'IN_REVIEW', 'SYNCED', 'CNPM-DEMO-REPORT-001'
FROM projects p
JOIN requirements r ON r.project_id = p.id AND r.jira_issue_key = 'CNPM-202'
JOIN features f ON f.project_id = p.id AND f.jira_epic_key = 'CNPM-EPIC-2'
JOIN sprints s ON s.project_id = p.id AND s.name = 'Sprint 2 - Requirements and Local'
JOIN users u ON u.username = 'member.test'
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.idempotency_key = 'CNPM-DEMO-REPORT-001');

UPDATE tasks t
JOIN projects p ON p.id = t.project_id
JOIN requirements r ON r.project_id = p.id AND r.jira_issue_key = 'CNPM-201'
SET t.requirement_id = r.id,
    t.sync_status = 'SYNCED'
WHERE p.name = 'CNPM Project Management Tool'
  AND t.title = 'Task mẫu được giao cho member.test';

INSERT INTO jira_issue_snapshots (
    project_id, jira_issue_id, jira_issue_key, summary, issue_type, status,
    url, remote_updated_at, last_synced_at, snapshot_hash, raw_snapshot
)
SELECT p.id, '12001', 'CNPM-201', 'Đăng nhập theo bốn vai trò', 'Story', 'Done',
       'https://demo.atlassian.net/browse/CNPM-201',
       '2026-09-12 08:00:00', '2026-09-13 01:00:00', 'demo-jira-201',
       '{"demo":true,"source":"JIRA","issueKey":"CNPM-201"}'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM jira_issue_snapshots j
                  WHERE j.project_id = p.id AND j.jira_issue_id = '12001');

INSERT INTO jira_issue_snapshots (
    project_id, jira_issue_id, jira_issue_key, summary, issue_type, status,
    url, remote_updated_at, last_synced_at, snapshot_hash, raw_snapshot
)
SELECT p.id, '12002', 'CNPM-202', 'Xem báo cáo tiến độ từ Jira và GitHub', 'Story', 'In Review',
       'https://demo.atlassian.net/browse/CNPM-202',
       '2026-09-12 09:00:00', '2026-09-13 01:00:00', 'demo-jira-202',
       '{"demo":true,"source":"JIRA","issueKey":"CNPM-202"}'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM jira_issue_snapshots j
                  WHERE j.project_id = p.id AND j.jira_issue_id = '12002');

INSERT INTO jira_issue_snapshots (
    project_id, jira_issue_id, jira_issue_key, summary, issue_type, status,
    url, remote_updated_at, last_synced_at, snapshot_hash, raw_snapshot
)
SELECT p.id, '12003', 'CNPM-203', 'Quản lý Task theo Sprint', 'Task', 'In Progress',
       'https://demo.atlassian.net/browse/CNPM-203',
       '2026-09-11 10:00:00', '2026-09-13 01:00:00', 'demo-jira-203',
       '{"demo":true,"source":"JIRA","issueKey":"CNPM-203"}'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM jira_issue_snapshots j
                  WHERE j.project_id = p.id AND j.jira_issue_id = '12003');

INSERT INTO jira_backlog_snapshots (
    project_id, jira_project_key, last_synced_at, snapshot_hash, raw_snapshot
)
SELECT p.id, 'CNPM', '2026-09-13 01:00:00', 'demo-jira-backlog-001',
       '{"demo":true,"source":"JIRA","issues":["CNPM-201","CNPM-202","CNPM-203"]}'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM jira_backlog_snapshots b WHERE b.project_id = p.id);

INSERT INTO jira_issues (
    task_id, jira_issue_id, jira_issue_key, url, remote_updated_at,
    last_synced_at, snapshot_hash, raw_snapshot
)
SELECT t.id, '12002', 'CNPM-202',
       'https://demo.atlassian.net/browse/CNPM-202',
       '2026-09-12 09:00:00', '2026-09-13 01:00:00',
       'demo-jira-task-202',
       '{"demo":true,"source":"JIRA","issueKey":"CNPM-202"}'
FROM tasks t
JOIN projects p ON p.id = t.project_id
WHERE p.name = 'CNPM Project Management Tool'
  AND t.idempotency_key = 'CNPM-DEMO-REPORT-001'
  AND NOT EXISTS (SELECT 1 FROM jira_issues j WHERE j.task_id = t.id);

INSERT INTO user_external_accounts (
    user_id, provider, external_user_id, external_login, avatar_url, profile_url
)
SELECT u.id, 'GITHUB', '900001', 'demo-team-leader',
       'https://avatars.githubusercontent.com/u/900001',
       'https://github.com/demo-team-leader'
FROM users u
WHERE u.username = 'leader.test'
  AND NOT EXISTS (SELECT 1 FROM user_external_accounts a
                  WHERE a.user_id = u.id AND a.provider = 'GITHUB');

INSERT INTO github_repositories (
    project_id, github_repository_id, full_name, default_branch, html_url,
    node_id, name, owner_github_user_id, owner_login,
    private_repository, archived, remote_updated_at, last_synced_at
)
SELECT p.id, 990001, 'demo/cnpm-project-support', 'main',
       'https://github.com/demo/cnpm-project-support',
       'MDQ6UmVwb3NpdG9yeTk5MDAwMQ==', 'cnpm-project-support', 900001,
       'demo', FALSE, FALSE, '2026-09-13 00:30:00', '2026-09-13 01:00:00'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM github_repositories r
                  WHERE r.github_repository_id = 990001);

INSERT INTO github_commits (
    repository_id, author_external_account_id, author_github_user_id, author_login,
    git_author_name, git_author_email, git_committer_name, git_committer_email,
    committer_at, sha, message, committed_at, html_url, additions, deletions,
    files_changed, parent_shas, is_reverted
)
SELECT r.id, a.id, 900001, 'demo-team-leader',
       'Demo Team Leader', 'demo-leader@example.invalid',
       'Demo Team Leader', 'demo-leader@example.invalid',
       '2026-09-10 02:00:00',
       '1111111111111111111111111111111111111111',
       'CNPM-201 implement demo login flow', '2026-09-10 02:00:00',
       'https://github.com/demo/cnpm-project-support/commit/1111111111111111111111111111111111111111',
       35, 8, 3, NULL, FALSE
FROM github_repositories r
JOIN user_external_accounts a ON a.provider = 'GITHUB' AND a.external_login = 'demo-team-leader'
WHERE r.github_repository_id = 990001
  AND NOT EXISTS (SELECT 1 FROM github_commits c
                  WHERE c.repository_id = r.id AND c.sha = '1111111111111111111111111111111111111111');

INSERT INTO github_commits (
    repository_id, author_external_account_id, author_github_user_id, author_login,
    git_author_name, git_author_email, git_committer_name, git_committer_email,
    committer_at, sha, message, committed_at, html_url, additions, deletions,
    files_changed, parent_shas, is_reverted
)
SELECT r.id, a.id, 900001, 'demo-team-leader',
       'Demo Team Leader', 'demo-leader@example.invalid',
       'Demo Team Leader', 'demo-leader@example.invalid',
       '2026-09-11 03:30:00',
       '2222222222222222222222222222222222222222',
       'CNPM-202 add reporting query', '2026-09-11 03:30:00',
       'https://github.com/demo/cnpm-project-support/commit/2222222222222222222222222222222222222222',
       62, 14, 5, '1111111111111111111111111111111111111111', FALSE
FROM github_repositories r
JOIN user_external_accounts a ON a.provider = 'GITHUB' AND a.external_login = 'demo-team-leader'
WHERE r.github_repository_id = 990001
  AND NOT EXISTS (SELECT 1 FROM github_commits c
                  WHERE c.repository_id = r.id AND c.sha = '2222222222222222222222222222222222222222');

INSERT INTO github_pull_requests (
    repository_id, github_pull_request_id, author_external_account_id,
    author_github_user_id, author_login, number, title, body, head_ref,
    head_sha, base_ref, state, draft, merged_at, merge_commit_sha,
    commit_count, additions, deletions, changed_files, closed_at,
    remote_created_at, html_url
)
SELECT r.id, 880001, a.id, 900001, 'demo-team-leader',
       21, 'CNPM-201 Demo login flow',
       'Synthetic Pull Request used for the final demo.', 'feature/demo-login',
       '1111111111111111111111111111111111111111', 'main', 'MERGED', FALSE,
       '2026-09-10 05:00:00', '3333333333333333333333333333333333333333',
       2, 35, 8, 3, '2026-09-10 05:00:00',
       '2026-09-10 01:00:00',
       'https://github.com/demo/cnpm-project-support/pull/21'
FROM github_repositories r
JOIN user_external_accounts a ON a.provider = 'GITHUB' AND a.external_login = 'demo-team-leader'
WHERE r.github_repository_id = 990001
  AND NOT EXISTS (SELECT 1 FROM github_pull_requests pr WHERE pr.github_pull_request_id = 880001);

INSERT INTO github_pull_requests (
    repository_id, github_pull_request_id, author_external_account_id,
    author_github_user_id, author_login, number, title, body, head_ref,
    head_sha, base_ref, state, draft, merged_at, merge_commit_sha,
    commit_count, additions, deletions, changed_files, closed_at,
    remote_created_at, html_url
)
SELECT r.id, 880002, a.id, 900001, 'demo-team-leader',
       22, 'CNPM-202 Reporting dashboard',
       'Synthetic open Pull Request used to demonstrate PR state counts.',
       'feature/demo-report', '2222222222222222222222222222222222222222',
       'main', 'OPEN', FALSE, NULL, NULL,
       1, 62, 14, 5, NULL, '2026-09-11 04:00:00',
       'https://github.com/demo/cnpm-project-support/pull/22'
FROM github_repositories r
JOIN user_external_accounts a ON a.provider = 'GITHUB' AND a.external_login = 'demo-team-leader'
WHERE r.github_repository_id = 990001
  AND NOT EXISTS (SELECT 1 FROM github_pull_requests pr WHERE pr.github_pull_request_id = 880002);

INSERT INTO task_commit_links (
    task_id, commit_id, link_source, linked_by_user_id, reason, matched_from
)
SELECT t.id, c.id, 'AUTO', leader.id,
       'Demo link from Jira issue key in commit message.', 'COMMIT_MESSAGE'
FROM tasks t
JOIN github_commits c ON c.sha = '1111111111111111111111111111111111111111'
JOIN users leader ON leader.username = 'leader.test'
WHERE t.title = 'Task mẫu được giao cho member.test'
  AND NOT EXISTS (SELECT 1 FROM task_commit_links l
                  WHERE l.task_id = t.id AND l.commit_id = c.id);

INSERT INTO task_commit_links (
    task_id, commit_id, link_source, linked_by_user_id, reason, matched_from
)
SELECT t.id, c.id, 'AUTO', leader.id,
       'Demo link from Jira issue key in commit message.', 'COMMIT_MESSAGE'
FROM tasks t
JOIN github_commits c ON c.sha = '2222222222222222222222222222222222222222'
JOIN users leader ON leader.username = 'leader.test'
WHERE t.idempotency_key = 'CNPM-DEMO-REPORT-001'
  AND NOT EXISTS (SELECT 1 FROM task_commit_links l
                  WHERE l.task_id = t.id AND l.commit_id = c.id);

INSERT INTO task_pr_links (
    task_id, pull_request_id, link_source, linked_by_user_id, reason, matched_from
)
SELECT t.id, pr.id, 'AUTO', leader.id,
       'Demo link from Jira issue key in Pull Request title.', 'PR_TITLE'
FROM tasks t
JOIN github_pull_requests pr ON pr.github_pull_request_id = 880001
JOIN users leader ON leader.username = 'leader.test'
WHERE t.title = 'Task mẫu được giao cho member.test'
  AND NOT EXISTS (SELECT 1 FROM task_pr_links l
                  WHERE l.task_id = t.id AND l.pull_request_id = pr.id);

INSERT INTO task_pr_links (
    task_id, pull_request_id, link_source, linked_by_user_id, reason, matched_from
)
SELECT t.id, pr.id, 'AUTO', leader.id,
       'Demo link from Jira issue key in Pull Request title.', 'PR_TITLE'
FROM tasks t
JOIN github_pull_requests pr ON pr.github_pull_request_id = 880002
JOIN users leader ON leader.username = 'leader.test'
WHERE t.idempotency_key = 'CNPM-DEMO-REPORT-001'
  AND NOT EXISTS (SELECT 1 FROM task_pr_links l
                  WHERE l.task_id = t.id AND l.pull_request_id = pr.id);

INSERT INTO sync_logs (
    project_id, provider, entity_type, entity_id, direction, status,
    retry_count, correlation_id, started_at, completed_at,
    idempotency_key, request_fingerprint
)
SELECT p.id, 'JIRA', 'PROJECT', 'CNPM', 'INBOUND', 'SUCCESS', 0,
       'demo-jira-sync-001', '2026-09-13 00:59:00', '2026-09-13 01:00:00',
       'demo-jira-project-sync-001', 'demo-jira-fingerprint-001'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM sync_logs s
                  WHERE s.project_id = p.id AND s.provider = 'JIRA'
                    AND s.idempotency_key = 'demo-jira-project-sync-001');

INSERT INTO sync_logs (
    project_id, provider, entity_type, entity_id, direction, status,
    retry_count, correlation_id, started_at, completed_at,
    idempotency_key, request_fingerprint
)
SELECT p.id, 'GITHUB', 'REPOSITORY', '990001', 'INBOUND', 'SUCCESS', 0,
       'demo-github-sync-001', '2026-09-13 00:59:00', '2026-09-13 01:00:00',
       'demo-github-repository-sync-001', 'demo-github-fingerprint-001'
FROM projects p
WHERE p.name = 'CNPM Project Management Tool'
  AND NOT EXISTS (SELECT 1 FROM sync_logs s
                  WHERE s.project_id = p.id AND s.provider = 'GITHUB'
                    AND s.idempotency_key = 'demo-github-repository-sync-001');

UPDATE sprints
SET last_synced_at = '2026-09-13 01:00:00',
    goal = 'Hoàn thiện demo quản lý yêu cầu, Task và báo cáo tích hợp.'
WHERE name = 'Sprint 2 - Requirements and Local';

