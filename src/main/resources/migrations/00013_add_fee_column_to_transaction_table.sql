--liquibase formatted sql

--changeset uk.gov.pay:add_fee_column_to_transaction_table

ALTER TABLE transaction
    ADD COLUMN fee BIGINT;