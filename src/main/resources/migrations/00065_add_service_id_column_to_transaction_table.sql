--liquibase formatted sql

--changeset uk.gov.pay:add_service_id_to_transaction_table
ALTER TABLE transaction
    ADD COLUMN service_id VARCHAR(32);

--rollback ALTER TABLE transaction DROP COLUMN service_id;

--changeset uk.gov.pay:add_index_on_service_id_to_transaction_table runInTransaction:false
CREATE INDEX CONCURRENTLY transaction_service_id_idx
    ON transaction(service_id);
