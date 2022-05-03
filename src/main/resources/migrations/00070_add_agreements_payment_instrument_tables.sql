--liquibase formatted sql

--changeset uk.gov.pay:create_table_agreement
CREATE TABLE agreement (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(26),

    -- API (public-api) authenticates and fetches data by card gateway account id
    -- needed until that's refactored
    gateway_account_id VARCHAR(255),
    service_id VARCHAR(32),
    reference VARCHAR(255),
    description VARCHAR(255),
    status text,
    live BOOLEAN,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc'),
    event_count INTEGER
);
--rollback drop table agreement;

--changeset uk.gov.pay:add_agreement_indexes_idx
ALTER TABLE agreement ADD CONSTRAINT agreements_external_id_key  UNIQUE (external_id);

-- used to group agreements by account
CREATE INDEX agreement_gateway_account_id_idx ON agreement(gateway_account_id);
CREATE INDEX agreement_service_id_idx ON agreement(service_id);

-- used to search, filter and ordering
CREATE INDEX agreement_status_idx ON agreement(status);
CREATE INDEX agreement_lower_reference_idx ON agreement USING btree(lower(reference));
CREATE INDEX agreement_created_date_idx ON agreement USING btree(created_date);

--changeset uk.gov.pay:create_table_payment_instrument
CREATE TABLE payment_instrument (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(26),
    agreement_external_id VARCHAR(26),
    email VARCHAR(254),
    cardholder_name VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    address_postcode VARCHAR(25),
    address_city VARCHAR(255),
    address_county VARCHAR(255),
    address_country VARCHAR(2),
    last_digits_card_number CHAR(4),
    expiry_date VARCHAR(6),
    card_brand TEXT,
    event_count INTEGER,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc')
);
--rollback drop table payment_instrument;

--changeset uk.gov.pay:add_payment_instrument_indexes_idx
ALTER TABLE payment_instrument ADD CONSTRAINT payment_instrument_external_id_key  UNIQUE (external_id);
CREATE INDEX payment_instrument_agreement_external_id_idx ON payment_instrument(agreement_external_id);
CREATE INDEX payment_instrument_created_date_idx ON payment_instrument(created_date);