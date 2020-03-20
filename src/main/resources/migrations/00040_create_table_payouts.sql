--liquibase formatted sql

--changeset uk.gov.pay:create_table_payouts
CREATE TABLE payouts (
    id BIGSERIAL PRIMARY KEY,
    gateway_payout_id VARCHAR(26),
    amount BIGINT NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc') NOT NULL,
    paid_out_date TIMESTAMP WITH TIME ZONE,
    statement_descriptor VARCHAR(255),
    status VARCHAR(26),
    type VARCHAR(26),
    version INTEGER NOT NULL DEFAULT 0
);
--rollback drop table payouts;

--changeset uk.gov.pay:add_index_gateway_payout_id
CREATE INDEX index_gateway_payout_id ON payouts(gateway_payout_id);
--rollback drop index index_gateway_payout_id;
