--liquibase formatted sql

--changeset uk.gov.pay:add_index_transaction_metadata_foreign_keys runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_metadata_transaction_id_metadatakey_id_idx
ON transaction_metadata USING btree(transaction_id, metadata_key_id);