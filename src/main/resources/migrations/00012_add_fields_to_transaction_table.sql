--liquibase formatted sql

--changeset uk.gov.pay:add_fields_to_transaction_table

ALTER TABLE transaction
    ADD COLUMN net_amount BIGINT,
    ADD COLUMN total_amount BIGINT,
    ADD COLUMN settlement_submitted_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN settled_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN refund_status VARCHAR(100),
    ADD COLUMN refund_amount_submitted BIGINT,
    ADD COLUMN refund_amount_available BIGINT;