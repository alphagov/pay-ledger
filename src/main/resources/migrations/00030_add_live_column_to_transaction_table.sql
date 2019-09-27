--liquibase formatted sql

--changeset uk.gov.pay:add_live_column_to_transaction_table

ALTER TABLE transaction
    ADD COLUMN live BOOLEAN;

--changeset uk.gov.pay:index_transaction_live_column runInTransaction:false

CREATE INDEX CONCURRENTLY transaction_live_idx ON transaction(live) WHERE live is true;
