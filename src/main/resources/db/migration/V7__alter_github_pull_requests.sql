ALTER TABLE github_pull_requests
ADD COLUMN github_pull_request_id BIGINT,
ADD COLUMN author_github_user_id BIGINT,
ADD COLUMN author_login VARCHAR(100),
ADD COLUMN body TEXT,
ADD COLUMN head_sha VARCHAR(64),
ADD COLUMN draft BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN merge_commit_sha VARCHAR(64),
ADD COLUMN commit_count INT,
ADD COLUMN additions INT NOT NULL DEFAULT 0,
ADD COLUMN deletions INT NOT NULL DEFAULT 0,
ADD COLUMN changed_files INT,
ADD COLUMN closed_at TIMESTAMP(6);