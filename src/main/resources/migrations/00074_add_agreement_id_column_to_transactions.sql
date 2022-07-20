--liquibase formatted sql

--changeset uk.gov.pay:add_agreement_id_to_transaction_table
ALTER TABLE transaction ADD COLUMN agreement_id VARCHAR(32);
--rollback ALTER TABLE transaction DROP COLUMN agreement_id;

--changeset uk.gov.pay:add_index_on_agreement_id_to_transaction_table runInTransaction:false
CREATE INDEX CONCURRENTLY transaction_agreement_id_idx ON transaction(agreement_id);