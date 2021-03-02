--liquibase formatted sql

--changeset uk.gov.pay:add_value_column_transaction_metadata_table

ALTER TABLE transaction_metadata ADD COLUMN value VARCHAR(256);

--rollback drop transaction_metadata;
