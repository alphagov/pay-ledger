--liquibase formatted sql

--changeset uk.gov.pay:rename_parent_transaction_id_in_transaction_table

ALTER TABLE transaction
    rename COLUMN parent_transaction_id to parent_external_id;

ALTER index transaction_parent_tx_id_idx rename to transaction_parent_external_id_idx;
