--liquibase formatted sql

--changeset uk.gov.pay:add_searchable_fields_to_transaction_table

ALTER TABLE transaction
    ADD COLUMN card_brand TEXT,
    ADD COLUMN last_digits_card_number CHAR(4),
    ADD COLUMN first_digits_card_number CHAR(6);

--rollback drop column card_brand, last_digits_card_number, first_digits_card_number;

--changeset uk.gov.pay:add_transaction_card_brand_idx
CREATE INDEX transaction_card_brand_idx ON transaction(card_brand)
--rollback drop index transaction_card_brand_idx;

--changeset uk.gov.pay:add_transaction_first_digits_card_number_idx
CREATE INDEX transaction_first_digits_card_number_idx ON transaction(first_digits_card_number)
--rollback drop index transaction_first_digits_card_number_idx;

--changeset uk.gov.pay:add_transaction_last_digits_card_number_idx
CREATE INDEX transaction_last_digits_card_number_idx ON transaction(last_digits_card_number)
--rollback drop index transaction_last_digits_card_number_idx;

--changeset uk.gov.pay:add_transaction_state_idx
CREATE INDEX transaction_state_idx ON transaction(state)
--rollback drop index transaction_state_idx;
