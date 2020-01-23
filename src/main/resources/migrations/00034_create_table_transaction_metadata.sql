--liquibase formatted sql

--changeset uk.gov.pay:create_table_transaction_metadata
CREATE TABLE transaction_metadata (
    id BIGSERIAL PRIMARY KEY,
    transaction_id bigserial NOT NULL,
    metadata_key_id bigserial NOT NULL
);
--rollback drop table transaction_metadata;

--changeset uk.gov.pay:add_transaction_metadata_transaction_id_fk
ALTER TABLE transaction_metadata ADD CONSTRAINT transaction_metadata_transaction_id_fk
FOREIGN KEY (transaction_id) REFERENCES transaction (id);
--rollback drop constraint add_transaction_metadata_transaction_id_fk;


--changeset uk.gov.pay:add_transaction_metadata_metadata_key_id_fk
ALTER TABLE transaction_metadata ADD CONSTRAINT transaction_metadata_metadata_key_id_fk
FOREIGN KEY (metadata_key_id) REFERENCES metadata_key (id);
--rollback drop constraint add_transaction_metadata_metadata_key_id_fk;