--liquibase formatted sql

--changeset uk.gov.pay:create_table_transaction_summary
CREATE TABLE transaction_summary (
    gateway_account_id VARCHAR(255),
    type VARCHAR(255),
    transaction_date TIMESTAMP WITH TIME ZONE,
    state TEXT,
    live BOOLEAN,
    moto BOOLEAN,
    total_amount_in_pence BIGINT,
    no_of_transactions BIGINT,
    fee BIGINT
);
--rollback drop table transaction_summary;

--changeset uk.gov.pay:add_unique_key_transaction_summary
ALTER TABLE transaction_summary
    ADD CONSTRAINT transaction_summmary_unique_key
    UNIQUE (gateway_account_id, type, transaction_date, state, live, moto);
--rollback drop constraint transaction_summary_unique_key;
