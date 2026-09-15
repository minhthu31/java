CREATE TABLE github_check_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    external_id BIGINT NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    pull_request_id BIGINT,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    html_url VARCHAR(500),
    started_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_github_check_run_repository_external
        UNIQUE (repository_id, external_id),
    CONSTRAINT fk_check_run_repository
        FOREIGN KEY (repository_id) REFERENCES github_repositories(id),
    CONSTRAINT fk_check_run_pull_request
        FOREIGN KEY (pull_request_id) REFERENCES github_pull_requests(id)
);

CREATE INDEX idx_github_check_run_repository_completed
    ON github_check_runs(repository_id, completed_at);

CREATE INDEX idx_github_check_run_commit_sha
    ON github_check_runs(repository_id, commit_sha);
