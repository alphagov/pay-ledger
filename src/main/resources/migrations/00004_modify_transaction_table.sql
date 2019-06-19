--liquibase formatted sql

--changeset uk.gov.pay:build_table_event

ALTER TABLE transaction

ADD COLUMN transaction_details jsonb,

DROP COLUMN return_url,
DROP COLUMN payment_provider,
DROP COLUMN language,
DROP COLUMN delayed_capture,
DROP COLUMN address_line1,
DROP COLUMN address_line2,
DROP COLUMN address_postcode,
DROP COLUMN address_city,
DROP COLUMN address_county,
DROP COLUMN address_country;
