--liquibase formatted sql

--changeset uk.gov.pay:create_table_transaction
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    gateway_account_id VARCHAR(255) NOT NULL,
    external_id VARCHAR(26),
    amount BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    return_url text NOT NULL,
    status text NOT NULL,
    payment_provider text NOT NULL,
    language VARCHAR(2) DEFAULT 'en',
    delayed_capture BOOLEAN DEFAULT false,
    email VARCHAR(254),
    cardholder_name VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    address_postcode VARCHAR(25),
    address_city VARCHAR(255),
    address_county VARCHAR(255),
    address_country VARCHAR(2),
    external_metadata jsonb,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc') NOT NULL
);
--rollback drop table transaction;

--changeset uk.gov.pay:add_transaction_gateway_account_id_idx
CREATE INDEX transaction_gateway_account_id_idx ON transaction(gateway_account_id)
--rollback drop index transaction_gateway_account_id_idx;
