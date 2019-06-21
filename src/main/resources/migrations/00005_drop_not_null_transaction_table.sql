--liquibase formatted sql

--changeset uk.gov.pay:alter_transaction_table_drop_not_null

ALTER TABLE transaction ALTER COLUMN gateway_account_id DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN amount DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN reference DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN description DROP NOT NULL;
