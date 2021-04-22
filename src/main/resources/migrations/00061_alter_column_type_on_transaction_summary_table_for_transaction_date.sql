--liquibase formatted sql

--changeset uk.gov.pay:change_column_type_for_transaction_date_on_transaction_summary

ALTER TABLE transaction_summary ALTER COLUMN transaction_date TYPE DATE;
