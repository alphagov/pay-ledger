--liquibase formatted sql

--changeset uk.gov.pay:drop_payout_foreign_key_from_transaction
ALTER TABLE transaction drop constraint transaction_gateway_payout_id_fkey;

--rollback ALTER TABLE transaction ADD CONSTRAINT transaction_gateway_payout_id_fkey FOREIGN KEY (gateway_payout_id) REFERENCES payout (gateway_payout_id);

