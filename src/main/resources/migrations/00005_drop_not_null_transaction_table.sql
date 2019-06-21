--liquibase formatted sql

--changeset uk.gov.pay:modify_transaction_table_drop_not_null_constraints

ALTER TABLE transaction ALTER COLUMN gateway_account_id DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN amount DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN reference DROP NOT NULL;
ALTER TABLE transaction ALTER COLUMN description DROP NOT NULL;
