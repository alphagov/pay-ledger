--changeset uk.gov.pay:drop_default_value_moto_column

ALTER TABLE transaction ALTER COLUMN moto DROP DEFAULT;