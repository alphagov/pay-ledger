--liquibase formatted sql

--changeset uk.gov.pay:add_index_on_transaction_gateway_account_id_and_created_date runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_gateway_account_id_and_created_date_idx ON transaction USING btree(gateway_account_id,created_date);
