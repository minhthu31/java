UPDATE tasks
SET sync_status = 'SYNCING'
WHERE sync_status = 'PENDING';

UPDATE tasks
SET sync_status = 'SYNC_FAILED'
WHERE sync_status = 'FAILED';

ALTER TABLE sync_logs
ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE sync_logs
ADD COLUMN request_fingerprint VARCHAR(128);

ALTER TABLE sync_logs
ADD CONSTRAINT uk_sync_log_idempotency
UNIQUE (project_id, provider, entity_type, entity_id, direction, idempotency_key);
