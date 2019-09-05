--liquibase formatted sql

--changeset uk.gov.pay:index_transaction_lower_email runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_lower_email_idx ON transaction USING GIN (lower(email) gin_trgm_ops);
