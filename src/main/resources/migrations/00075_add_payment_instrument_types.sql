--liquibase formatted sql

--changeset uk.gov.pay:add_missing_common_types_to_payment_instrument
ALTER TABLE payment_instrument ADD COLUMN type TEXT;
ALTER TABLE payment_instrument ADD COLUMN first_digits_card_number CHAR(6);
ALTER TABLE payment_instrument ADD COLUMN card_type VARCHAR(20);
