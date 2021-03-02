--liquibase formatted sql

--changeset uk.gov.pay:add_index_transaction_metadata_value_column runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_metadata_lower_value_idx
ON transaction_metadata USING btree((lower(value)));
