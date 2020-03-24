--liquibase formatted sql

--changeset uk.gov.pay:add_payouts_foreign_key_to_transaction
ALTER TABLE transaction ADD COLUMN gateway_payout_id VARCHAR(50) REFERENCES payouts(gateway_payout_id);
--rollback ALTER TABLE transaction DROP COLUMN gateway_payout_id; 

--changeset uk.gov.pay:create_index_on_transaction_gateway_payout_id runInTransaction:false
CREATE INDEX CONCURRENTLY transaction_gateway_payout_id_idx ON transaction(gateway_payout_id);
--rollback DROP INDEX transaction_gateway_payout_id_idx;
