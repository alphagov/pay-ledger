--liquibase formatted sql

--changeset uk.gov.pay:add_state_to_payout
ALTER TABLE payout ADD COLUMN state TEXT;

--rollback ALTER TABLE payout DROP COLUMN state;

