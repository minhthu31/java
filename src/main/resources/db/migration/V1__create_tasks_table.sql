CREATE TABLE tasks (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    acceptance_criteria TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    project_id VARCHAR(50),
    feature_id VARCHAR(50),
    created_by_id VARCHAR(50) NOT NULL,
    assignee_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    sync_status VARCHAR(50) NOT NULL DEFAULT 'NOT_SYNCED'
);