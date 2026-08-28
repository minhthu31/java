ALTER TABLE projects ADD COLUMN jira_last_synced_at TIMESTAMP(6);
ALTER TABLE sprints ADD COLUMN last_synced_at TIMESTAMP(6);
ALTER TABLE sprints ADD COLUMN goal VARCHAR(1000);

CREATE TABLE jira_issue_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    jira_issue_id VARCHAR(100) NOT NULL,
    jira_issue_key VARCHAR(50) NOT NULL,
    summary VARCHAR(500),
    issue_type VARCHAR(100),
    status VARCHAR(100),
    url VARCHAR(500),
    remote_updated_at TIMESTAMP(6),
    last_synced_at TIMESTAMP(6) NOT NULL,
    snapshot_hash VARCHAR(128),
    raw_snapshot JSON,
    CONSTRAINT uk_jira_snapshot_project_id UNIQUE (project_id, jira_issue_id),
    CONSTRAINT uk_jira_snapshot_project_key UNIQUE (project_id, jira_issue_key),
    CONSTRAINT fk_jira_snapshot_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE jira_backlog_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    jira_project_key VARCHAR(30) NOT NULL,
    last_synced_at TIMESTAMP(6) NOT NULL,
    snapshot_hash VARCHAR(128),
    raw_snapshot JSON NOT NULL,
    CONSTRAINT uk_jira_backlog_project UNIQUE (project_id),
    CONSTRAINT fk_jira_backlog_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX idx_jira_issue_snapshot_project ON jira_issue_snapshots(project_id);
