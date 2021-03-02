--liquibase formatted sql

--changeset uk.gov.pay:add_unique_constraint_on_transaction_id_and_metadata_key_id

ALTER TABLE transaction_metadata ADD CONSTRAINT transaction_id_and_metadata_key_id_key UNIQUE (transaction_id, metadata_key_id);
