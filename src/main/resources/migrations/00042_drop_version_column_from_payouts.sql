--liquibase formatted sql

--changeset uk.gov.pay:drop_version_column_from_payouts
ALTER TABLE payouts DROP COLUMN version;
--rollback ALTER TABLE payouts ADD COLUMN version INTEGER NOT NULL DEFAULT 0
