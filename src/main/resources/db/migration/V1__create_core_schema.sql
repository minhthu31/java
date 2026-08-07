CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE user_external_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    external_user_id VARCHAR(100) NOT NULL,
    external_login VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_external_account UNIQUE (provider, external_user_id),
    CONSTRAINT fk_external_account_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE student_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    leader_user_id BIGINT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_group_leader FOREIGN KEY (leader_user_id) REFERENCES users(id)
);

CREATE TABLE group_members (
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL DEFAULT 'TEAM_MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at TIMESTAMP(6),
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES student_groups(id),
    CONSTRAINT fk_group_member_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE group_lecturers (
    group_id BIGINT NOT NULL,
    lecturer_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (group_id, lecturer_user_id),
    CONSTRAINT fk_group_lecturer_group FOREIGN KEY (group_id) REFERENCES student_groups(id),
    CONSTRAINT fk_group_lecturer_user FOREIGN KEY (lecturer_user_id) REFERENCES users(id)
);

CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    jira_site_url VARCHAR(500),
    jira_project_id VARCHAR(100),
    jira_project_key VARCHAR(30),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_project_jira_key UNIQUE (jira_site_url, jira_project_key),
    CONSTRAINT fk_project_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
);

CREATE TABLE integration_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    base_url VARCHAR(500),
    account_identifier VARCHAR(255),
    encrypted_secret TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_CHECKED',
    last_checked_at TIMESTAMP(6),
    last_error_code VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_project_provider_config UNIQUE (project_id, provider),
    CONSTRAINT fk_integration_config_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE requirements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    jira_issue_key VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    actor VARCHAR(255),
    priority VARCHAR(20),
    precondition TEXT,
    main_flow TEXT,
    alternative_flow TEXT,
    exception_flow TEXT,
    postcondition TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_requirement_jira_key UNIQUE (project_id, jira_issue_key),
    CONSTRAINT fk_requirement_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_feature_id BIGINT,
    jira_epic_key VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_feature_jira_epic UNIQUE (project_id, jira_epic_key),
    CONSTRAINT fk_feature_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_feature_parent FOREIGN KEY (parent_feature_id) REFERENCES features(id)
);

CREATE TABLE sprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    jira_sprint_id BIGINT,
    name VARCHAR(255) NOT NULL,
    state VARCHAR(30) NOT NULL,
    start_date TIMESTAMP(6),
    end_date TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_project_jira_sprint UNIQUE (project_id, jira_sprint_id),
    CONSTRAINT fk_sprint_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    requirement_id BIGINT,
    feature_id BIGINT,
    sprint_id BIGINT,
    assignee_user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    acceptance_criteria TEXT NOT NULL,
    issue_type VARCHAR(30) NOT NULL,
    classification VARCHAR(30),
    priority VARCHAR(20) NOT NULL,
    deadline TIMESTAMP(6),
    status VARCHAR(30) NOT NULL DEFAULT 'TO_DO',
    sync_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SYNCED',
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_task_requirement FOREIGN KEY (requirement_id) REFERENCES requirements(id),
    CONSTRAINT fk_task_feature FOREIGN KEY (feature_id) REFERENCES features(id),
    CONSTRAINT fk_task_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_user_id) REFERENCES users(id)
);

CREATE TABLE jira_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    jira_issue_id VARCHAR(100) NOT NULL UNIQUE,
    jira_issue_key VARCHAR(50) NOT NULL UNIQUE,
    url VARCHAR(500) NOT NULL,
    remote_updated_at TIMESTAMP(6),
    last_synced_at TIMESTAMP(6) NOT NULL,
    snapshot_hash VARCHAR(128),
    raw_snapshot JSON,
    CONSTRAINT fk_jira_issue_task FOREIGN KEY (task_id) REFERENCES tasks(id)
);

CREATE TABLE github_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    github_repository_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL UNIQUE,
    default_branch VARCHAR(255) NOT NULL,
    html_url VARCHAR(500) NOT NULL,
    last_synced_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_repository_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE github_commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    author_external_account_id BIGINT,
    sha VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    committed_at TIMESTAMP(6) NOT NULL,
    html_url VARCHAR(500) NOT NULL,
    additions INT,
    deletions INT,
    files_changed INT,
    is_reverted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_repository_commit UNIQUE (repository_id, sha),
    CONSTRAINT fk_commit_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id),
    CONSTRAINT fk_commit_author FOREIGN KEY (author_external_account_id) REFERENCES user_external_accounts(id)
);

CREATE TABLE github_pull_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    author_external_account_id BIGINT,
    number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    head_ref VARCHAR(255) NOT NULL,
    base_ref VARCHAR(255) NOT NULL,
    state VARCHAR(30) NOT NULL,
    merged_at TIMESTAMP(6),
    html_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_repository_pr UNIQUE (repository_id, number),
    CONSTRAINT fk_pr_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id),
    CONSTRAINT fk_pr_author FOREIGN KEY (author_external_account_id) REFERENCES user_external_accounts(id)
);

CREATE TABLE workflow_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    commit_id BIGINT,
    external_run_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    conclusion VARCHAR(30),
    started_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    html_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_workflow_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id),
    CONSTRAINT fk_workflow_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id)
);

CREATE TABLE task_commit_links (
    task_id BIGINT NOT NULL,
    commit_id BIGINT NOT NULL,
    link_source VARCHAR(20) NOT NULL,
    linked_by_user_id BIGINT,
    reason VARCHAR(500),
    linked_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (task_id, commit_id),
    CONSTRAINT fk_task_commit_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_commit_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id),
    CONSTRAINT fk_task_commit_user FOREIGN KEY (linked_by_user_id) REFERENCES users(id)
);

CREATE TABLE task_pr_links (
    task_id BIGINT NOT NULL,
    pull_request_id BIGINT NOT NULL,
    link_source VARCHAR(20) NOT NULL,
    linked_by_user_id BIGINT,
    reason VARCHAR(500),
    linked_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (task_id, pull_request_id),
    CONSTRAINT fk_task_pr_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_pr_pr FOREIGN KEY (pull_request_id) REFERENCES github_pull_requests(id),
    CONSTRAINT fk_task_pr_user FOREIGN KEY (linked_by_user_id) REFERENCES users(id)
);

CREATE TABLE sync_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    correlation_id VARCHAR(100) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6),
    CONSTRAINT fk_sync_log_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    group_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    old_value JSON,
    new_value JSON,
    result VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_activity_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
);

CREATE TABLE srs_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,
    generated_by_user_id BIGINT NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    source_synced_at TIMESTAMP(6),
    file_url VARCHAR(1000),
    checksum VARCHAR(128),
    CONSTRAINT uk_project_srs_version UNIQUE (project_id, version),
    CONSTRAINT fk_srs_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_srs_generator FOREIGN KEY (generated_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_task_project_status ON tasks(project_id, status);
CREATE INDEX idx_task_assignee_status ON tasks(assignee_user_id, status);
CREATE INDEX idx_commit_author_time ON github_commits(author_external_account_id, committed_at);
CREATE INDEX idx_sync_project_status ON sync_logs(project_id, status);
CREATE INDEX idx_activity_entity ON activity_logs(entity_type, entity_id, created_at);
