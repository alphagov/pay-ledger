--liquibase formatted sql

--changeset uk.gov.pay:index_transaction_gateway_transaction_id runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_gateway_transaction_id_idx ON transaction USING btree(gateway_transaction_id);