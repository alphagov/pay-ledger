--liquibase formatted sql

--changeset uk.gov.pay:add_refund_related_fields_to_transaction_table

CREATE type transaction_type as enum ('PAYMENT', 'REFUND');

ALTER TABLE transaction
    ADD COLUMN gateway_transaction_id text,
    ADD COLUMN type transaction_type ,
    ADD COLUMN origin_transaction_id VARCHAR(26) references transaction(external_id);

CREATE index transaction_origin_id_idx on transaction(origin_transaction_id);