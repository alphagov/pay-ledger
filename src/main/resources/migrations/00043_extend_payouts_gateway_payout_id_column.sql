--liquibase formatted sql

--changeset uk.gov.pay:extend_gateway_payout_id_column
ALTER TABLE payouts ALTER COLUMN gateway_payout_id TYPE VARCHAR(50);
--rollback ALTER TABLE payouts ALTER COLUMN gateway_payout_id TYPE VARCHAR(26)
