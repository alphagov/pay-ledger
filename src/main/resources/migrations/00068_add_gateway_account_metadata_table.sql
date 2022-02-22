--liquibase formatted sql

--changeset uk.gov.pay:create_table_gateway_account_metadata
CREATE TABLE gateway_account_metadata (
     id BIGSERIAL PRIMARY KEY,
     gateway_account_id VARCHAR(255) NOT NULL,
     metadata_key_id bigserial NOT NULL
);

--changeset uk.gov.pay:add_gateway_account_metadata_metadata_key_id_fk
ALTER TABLE gateway_account_metadata ADD CONSTRAINT gateway_account_metadata_metadata_key_id_fk
    FOREIGN KEY (metadata_key_id) REFERENCES metadata_key (id);

--changeset uk.gov.pay:add_unique_constraint_on_gateway_account_id_and_metadata_key_id
ALTER TABLE gateway_account_metadata ADD CONSTRAINT gateway_account_id_and_metadata_key_id_key UNIQUE (gateway_account_id, metadata_key_id);
