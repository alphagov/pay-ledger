--liquibase formatted sql

--changeset uk.gov.pay:remove_transaction_external_metadata.sql

ALTER TABLE transaction DROP COLUMN external_metadata;