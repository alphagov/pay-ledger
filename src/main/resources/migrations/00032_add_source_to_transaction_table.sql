--liquibase formatted sql

--changeset uk.gov.pay:add_source_to_transaction_table
CREATE type source as enum ('CARD_API', 'CARD_PAYMENT_LINK', 'CARD_EXTERNAL_TELEPHONE');

ALTER TABLE transaction
    ADD COLUMN source source;