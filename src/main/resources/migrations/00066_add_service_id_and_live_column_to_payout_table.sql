--liquibase formatted sql

--changeset uk.gov.pay:add_service_id_to_payout_table
ALTER TABLE payout
    ADD COLUMN service_id VARCHAR(32),
    ADD COLUMN live BOOLEAN;

--rollback ALTER TABLE payout DROP COLUMN service_id, DROP COLUMN live;

--changeset uk.gov.pay:add_index_on_service_id_payout_table runInTransaction:false
CREATE INDEX CONCURRENTLY payout_service_id_idx
    ON payout(service_id);

--changeset uk.gov.pay:add_partial_index_on_live_payout_table runInTransaction:false
CREATE INDEX CONCURRENTLY payout_live_idx
    ON payout(live)
    WHERE live is true;
