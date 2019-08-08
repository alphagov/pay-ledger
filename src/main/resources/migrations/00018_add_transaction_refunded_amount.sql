--liquibase formatted sql

--changeset uk.gov.pay:rename_transaction_refunded_amount.sql

ALTER TABLE transaction
    ADD COLUMN refund_amount_refunded BIGINT;