CREATE TABLE github_pull_request_commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pull_request_id BIGINT NOT NULL,
    commit_id BIGINT NOT NULL,
    commit_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_github_pr_commit UNIQUE (pull_request_id, commit_id),
    CONSTRAINT fk_github_pr_commit_pr
        FOREIGN KEY (pull_request_id) REFERENCES github_pull_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_github_pr_commit_commit
        FOREIGN KEY (commit_id) REFERENCES github_commits(id) ON DELETE CASCADE
);

CREATE INDEX idx_github_pr_commit_order
    ON github_pull_request_commits(pull_request_id, commit_order);
