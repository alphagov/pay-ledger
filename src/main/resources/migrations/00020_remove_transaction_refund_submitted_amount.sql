--liquibase formatted sql

--changeset uk.gov.pay:remove_transaction_refund_submitted_amount

ALTER TABLE transaction
    DROP COLUMN refund_amount_submitted;