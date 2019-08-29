--liquibase formatted sql

--changeset uk.gov.pay:index_created_date_for_order_by runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS created_date_idx ON transaction USING btree(created_date);