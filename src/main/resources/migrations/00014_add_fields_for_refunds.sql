--liquibase formatted sql

--changeset uk.gov.pay:add_refund_related_fields_to_transaction_table

CREATE type transaction_type as enum ('PAYMENT', 'REFUND');

ALTER TABLE transaction
    ADD COLUMN gateway_transaction_id text,
    ADD COLUMN type transaction_type ,
    ADD COLUMN parent_transaction_id VARCHAR(26) references transaction(external_id);

CREATE index transaction_parent_tx_id_idx on transaction(parent_transaction_id);
