--liquibase formatted sql

--changeset uk.gov.pay:index_transaction_type_for_filtering runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_type_idx ON transaction USING btree(type);