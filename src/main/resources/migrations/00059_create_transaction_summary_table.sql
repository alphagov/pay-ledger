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

CREATE TABLE transaction_summary_refresh_status
(
    last_refresh_date TIMESTAMP WITH TIME ZONE NOT NULL
);

insert into transaction_summary_refresh_status values('01-Sep-16 0:0:0 UTC');