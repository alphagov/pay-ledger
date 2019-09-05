--liquibase formatted sql

--changeset uk.gov.pay:index_transaction_lower_cardholder_name runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_lower_cardholder_name_idx ON transaction USING GIN (lower(cardholder_name) gin_trgm_ops);