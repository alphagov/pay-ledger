--liquibase formatted sql

--changeset uk.gov.pay:add_event_count_payout_details_to_payout
ALTER TABLE payouts ADD COLUMN event_count INTEGER;
ALTER TABLE payouts ADD COLUMN payout_details jsonb;

--rollback ALTER TABLE transaction DROP COLUMN event_count;
--rollback ALTER TABLE transaction DROP COLUMN payout_details;
