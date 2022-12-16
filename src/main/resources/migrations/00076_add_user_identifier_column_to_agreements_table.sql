--liquibase formatted sql

--changeset uk.gov.pay:add_user_identifier_column_to_agreements_table
ALTER TABLE agreement ADD COLUMN user_identifier VARCHAR(255);

--changeset uk.gov.pay:add_user_identifier_index_to_agreements_table runInTransaction:false
CREATE INDEX CONCURRENTLY agreement_user_identifier_idx ON agreement(user_identifier);