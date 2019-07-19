--liquibase formatted sql

--changeset uk.gov.pay:add_fee_columns_to_transaction_table

ALTER TABLE transaction
    ADD COLUMN fee BIGINT,
    ADD COLUMN corporate_surcharge BIGINT;