--liquibase formatted sql

--changeset uk.gov.pay:drop_index_transaction_gateway_account_id runInTransaction:false

DROP INDEX CONCURRENTLY IF EXISTS transaction_gateway_account_id_idx;
