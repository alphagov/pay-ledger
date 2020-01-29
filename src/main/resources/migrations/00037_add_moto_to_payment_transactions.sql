--liquibase formatted sql

--changeset uk.gov.pay:add_moto_column_to_transaction_table

ALTER TABLE transaction ADD COLUMN moto BOOLEAN;

--changeset uk.gov.pay:default_value_moto_column

ALTER TABLE transaction ALTER COLUMN moto SET DEFAULT false;
