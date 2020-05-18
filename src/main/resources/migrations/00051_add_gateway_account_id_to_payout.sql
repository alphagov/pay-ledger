--liquibase formatted sql

--changeset uk.gov.pay:add_gateway_account_id_to_payout_table
ALTER TABLE payout ADD COLUMN gateway_account_id VARCHAR(255);
CREATE INDEX CONCURRENTLY IF NOT EXISTS payout_gateway_account_id_idx ON payout USING btree(gateway_account_id);

--rollback ALTER TABLE payouts DROP COLUMN gateway_account_id;
