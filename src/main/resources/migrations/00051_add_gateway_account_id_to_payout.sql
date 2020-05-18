--liquibase formatted sql

--changeset uk.gov.pay:add_gateway_account_id_to_payout_table
ALTER TABLE payout ADD COLUMN gateway_account_id VARCHAR(255);

--rollback ALTER TABLE payouts DROP COLUMN gateway_account_id;

--changeset uk.gov.pay:add_index_on_gateway_account_id_to_payout_table
CREATE INDEX payout_gateway_account_id_idx ON payout(gateway_account_id);

--rollback drop index payout_gateway_account_id_idx;