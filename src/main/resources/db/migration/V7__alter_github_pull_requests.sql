ALTER TABLE github_pull_requests ADD COLUMN github_pull_request_id BIGINT;
ALTER TABLE github_pull_requests ADD COLUMN author_external_account_id BIGINT;
ALTER TABLE github_pull_requests ADD COLUMN author_github_user_id BIGINT;
ALTER TABLE github_pull_requests ADD COLUMN author_login VARCHAR(100);
ALTER TABLE github_pull_requests ADD COLUMN body TEXT;
ALTER TABLE github_pull_requests ADD COLUMN head_sha VARCHAR(64);
ALTER TABLE github_pull_requests ADD COLUMN draft BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE github_pull_requests ADD COLUMN merge_commit_sha VARCHAR(64);
ALTER TABLE github_pull_requests ADD COLUMN commit_count INT;
ALTER TABLE github_pull_requests ADD COLUMN additions INT NOT NULL DEFAULT 0;
ALTER TABLE github_pull_requests ADD COLUMN deletions INT NOT NULL DEFAULT 0;
ALTER TABLE github_pull_requests ADD COLUMN changed_files INT;
ALTER TABLE github_pull_requests ADD COLUMN closed_at TIMESTAMP(6);

ALTER TABLE github_pull_requests ADD CONSTRAINT uk_github_pull_request_id UNIQUE (github_pull_request_id);