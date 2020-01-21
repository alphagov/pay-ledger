--liquibase formatted sql

--changeset uk.gov.pay:add_metadata_table
CREATE table metadata (
  id BIGSERIAL PRIMARY KEY,
  name varchar(255)
);

--changeset uk.gov.pay:add_transaction_metadata_table
CREATE TABLE transaction_metadata(
  id BIGSERIAL PRIMARY KEY,
  transaction_id INT,
  metadata_id INT
);

--changeset uk.gov.pay:add_foreign_key_transaction_metadata_to_transaction
ALTER TABLE transaction_metadata ADD CONSTRAINT transaction_metadata_fk_transaction_fk FOREIGN KEY (transaction_id) REFERENCES transaction (id);


--changeset uk.gov.pay:add_foreign_key_transaction_metadata_to_metadata
ALTER TABLE transaction_metadata ADD CONSTRAINT transaction_metadata_fk_metadata_fk FOREIGN KEY (metadata_id) REFERENCES metadata (id);

--changeset uk.gov.pay:add_index_on_metadata_name
create index idx_metadata_name on metadata (name);

--changeset uk.gov.pay:add_index_on_transaction_metadata_md_id
create index idx_tran_medta_md_id on transaction_metadata (metadata_id );

--changeset uk.gov.pay:add_index_on_transaction_metadata_tx_id
create index idx_tran_medta_tx_id on transaction_metadata (transaction_id );

--changeset uk.gov.pay:add_partial_index_on_transaction_external_metadata
create index idx_transaction_ext_metadata on transaction(transaction_details) where transaction_details ? 'external_metadata';

--changeset uk.gov.pay:add_index_on_metadata_name
create index idx_metadata_name on metadata (name);