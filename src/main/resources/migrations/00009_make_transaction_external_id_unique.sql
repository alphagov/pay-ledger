--liquibase formatted sql

--changeset uk.gov.pay:add_unique_constraint_on_external_id

ALTER TABLE transaction ADD CONSTRAINT external_id_key  UNIQUE (external_id);