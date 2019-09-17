--liquibase formatted sql

--changeset uk.gov.pay:multicolumn_index_transaction_gateway_account_id_and_type runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_gateway_account_id_and_type_idx ON transaction USING btree(gateway_account_id, type);
