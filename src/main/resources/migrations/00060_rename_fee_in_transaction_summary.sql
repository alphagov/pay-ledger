--liquibase formatted sql

--changeset uk.gov.pay:rename_fee_in_transaction_summary_table

ALTER TABLE transaction_summary
    rename COLUMN fee TO total_fee_in_pence;
