--liquibase formatted sql

--changeset uk.gov.pay:partial_index_transaction_external_metadata runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_external_metadata_idx on transaction(transaction_details)
where transaction_details ?? 'external_metadata';
--rollback drop index CONCURRENTLY transaction_external_metadata_idx;