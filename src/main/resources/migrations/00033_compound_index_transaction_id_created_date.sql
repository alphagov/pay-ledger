--liquibase formatted sql

--changeset uk.gov.pay:compound_index_transaction_created_date_id runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS transaction_created_date_id_idx ON transaction USING btree(created_date, id);