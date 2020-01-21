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