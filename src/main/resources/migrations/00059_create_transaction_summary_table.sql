--liquibase formatted sql

--changeset uk.gov.pay:create_transaction_summary_table

CREATE TABLE transaction_summary
(
    gateway_account_id    VARCHAR(255) NOT NULL,
    transaction_date      date,
    state                 text,
    live                  boolean,
    total_amount_in_pence BIGINT,
    no_of_transactions    BIGINT
);

ALTER TABLE transaction_summary
    ADD CONSTRAINT transaction_summmary_unique_key UNIQUE (gateway_account_id, transaction_date, state, live);
