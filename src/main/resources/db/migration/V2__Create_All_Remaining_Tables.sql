-- 1. TÀI KHOẢN NGOÀI
CREATE TABLE user_external_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    jira_account_id VARCHAR(128),
    github_user_id VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_external_users FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. NHÓM & DỰ ÁN
CREATE TABLE student_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    leader_user_id BIGINT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_groups_leader FOREIGN KEY (leader_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_members (
    group_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_members_group FOREIGN KEY (group_id) REFERENCES student_groups(id),
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_lecturers (
    group_id INT NOT NULL,
    lecturer_user_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, lecturer_user_id),
    CONSTRAINT fk_lecturers_group FOREIGN KEY (group_id) REFERENCES student_groups(id),
    CONSTRAINT fk_lecturers_user FOREIGN KEY (lecturer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    jira_project_id VARCHAR(50),
    jira_project_key VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. YÊU CẦU, TASK & JIRA
CREATE TABLE requirements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    jira_issue_key VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    actor VARCHAR(100),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reqs_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE features (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    jira_epic_key VARCHAR(50),
    name VARCHAR(150) NOT NULL,
    parent_feature_id INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_features_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_features_parent FOREIGN KEY (parent_feature_id) REFERENCES features(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sprints (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    jira_sprint_id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'FUTURE',
    start_date DATETIME,
    end_date DATETIME,
    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    requirement_id INT,
    feature_id INT,
    sprint_id INT,
    assignee_user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    issue_type VARCHAR(30) NOT NULL DEFAULT 'Task',
    priority VARCHAR(20) NOT NULL DEFAULT 'Medium',
    deadline DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'To Do',
    sync_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SYNCED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_req FOREIGN KEY (requirement_id) REFERENCES requirements(id),
    CONSTRAINT fk_tasks_feature FOREIGN KEY (feature_id) REFERENCES features(id),
    CONSTRAINT fk_tasks_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE jira_issues (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL UNIQUE,
    jira_issue_id VARCHAR(64) NOT NULL,
    jira_issue_key VARCHAR(50) NOT NULL UNIQUE,
    last_synced_at DATETIME,
    CONSTRAINT fk_jira_task FOREIGN KEY (task_id) REFERENCES tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. GITHUB & AUTO-TEST
CREATE TABLE github_repositories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    full_name VARCHAR(150) NOT NULL UNIQUE,
    default_branch VARCHAR(50) NOT NULL DEFAULT 'main',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repos_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE github_commits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT NOT NULL,
    sha VARCHAR(40) NOT NULL UNIQUE,
    author_external_id VARCHAR(128),
    message TEXT,
    additions INT NOT NULL DEFAULT 0,
    deletions INT NOT NULL DEFAULT 0,
    is_reverted BOOLEAN NOT NULL DEFAULT FALSE,
    committed_at DATETIME,
    CONSTRAINT fk_commits_repo FOREIGN KEY (repository_id) REFERENCES github_repositories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE github_pull_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT NOT NULL,
    number INT NOT NULL,
    head_ref VARCHAR(100),
    state VARCHAR(20) NOT NULL DEFAULT 'open',
    html_url VARCHAR(255),
    created_at DATETIME,
    CONSTRAINT fk_prs_repo FOREIGN KEY (repository_id) REFERENCES github_repositories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workflow_runs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    repository_id INT NOT NULL,
    commit_id INT,
    external_run_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(30),
    conclusion VARCHAR(30),
    run_at DATETIME,
    CONSTRAINT fk_workflows_repo FOREIGN KEY (repository_id) REFERENCES github_repositories(id),
    CONSTRAINT fk_workflows_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_commit_links (
    task_id INT NOT NULL,
    commit_id INT NOT NULL,
    link_source VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    linked_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, commit_id),
    CONSTRAINT fk_tcl_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_tcl_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id),
    CONSTRAINT fk_tcl_user FOREIGN KEY (linked_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_pr_links (
    task_id INT NOT NULL,
    pr_id INT NOT NULL,
    link_source VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, pr_id),
    CONSTRAINT fk_tpr_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_tpr_pr FOREIGN KEY (pr_id) REFERENCES github_pull_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. LOGS & XUẤT BÁO CÁO
CREATE TABLE sync_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_synclogs_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    group_id INT,
    action VARCHAR(100) NOT NULL,
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_actlogs_user FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT fk_actlogs_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE srs_versions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    version VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_srs_project FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;