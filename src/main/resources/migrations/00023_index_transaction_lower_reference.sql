--liquibase formatted sql

--changeset uk.gov.pay:index_transaction_lower_reference runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_lower_reference_idx ON transaction USING btree(lower(reference));