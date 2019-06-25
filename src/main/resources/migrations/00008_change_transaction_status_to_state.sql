--liquibase formatted sql

--changeset uk.gov.pay:transaction_status_to_state

ALTER TABLE transaction RENAME status TO state;