--liquibase formatted sql

--changeset uk.gov.pay:drop_index_gateway_payout_id
DROP INDEX index_gateway_payout_id;
--rollback CREATE INDEX index_gateway_payout_id ON payouts(gateway_payout_id);

--changeset uk.gov.pay:constraint_index_gateway_payout_id
ALTER TABLE payouts ADD CONSTRAINT gateway_payout_id_unique  UNIQUE (gateway_payout_id);
--rollback ALTER TABLE payouts DROP CONSTRAINT gateway_payout_id_unique;
