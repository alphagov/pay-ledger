--liquibase formatted sql

--changeset uk.gov.pay:gin_index_transaction_reference runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_reference_gin_idx ON transaction USING GIN (lower(reference) gin_trgm_ops);
