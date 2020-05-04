--liquibase formatted sql

--changeset uk.gov.pay:rename_payouts_table
ALTER TABLE payouts RENAME to payout;

--rollback ALTER TABLE payout rename to payouts;
