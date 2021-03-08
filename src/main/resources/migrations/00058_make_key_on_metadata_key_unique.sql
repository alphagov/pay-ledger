--liquibase formatted sql

--changeset uk.gov.pay:add_unique_constraint_on_key_on_metadata_key

ALTER TABLE metadata_key ADD CONSTRAINT metadata_key_unique_key UNIQUE (key);
