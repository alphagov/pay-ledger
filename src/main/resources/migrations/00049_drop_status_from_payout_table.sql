--liquibase formatted sql

--changeset uk.gov.pay:drop_status_from_payout
ALTER TABLE payout DROP COLUMN status;

--rollback ALTER TABLE payout ADD COLUMN status VARCHAR(26)

