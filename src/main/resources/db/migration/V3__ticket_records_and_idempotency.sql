ALTER TABLE kitchen_ticket
    ADD COLUMN recall_count INTEGER NOT NULL DEFAULT 0 CHECK (recall_count >= 0);

ALTER TABLE idempotency_record
    ADD COLUMN user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE;

ALTER TABLE idempotency_record
    DROP CONSTRAINT idempotency_record_idempotency_key_key;

ALTER TABLE idempotency_record
    ADD CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idempotency_key);

CREATE INDEX idempotency_created_idx ON idempotency_record(created_at);

CREATE TABLE token_counter (
    business_day DATE PRIMARY KEY,
    next_token INTEGER NOT NULL CHECK (next_token > 0)
);

CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_entry is append-only';
END;
$$;

CREATE TRIGGER audit_entry_append_only
    BEFORE UPDATE OR DELETE ON audit_entry
    FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
