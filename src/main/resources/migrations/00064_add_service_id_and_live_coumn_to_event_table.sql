--liquibase formatted sql

--changeset uk.gov.pay:add_service_id_and_live_to_event_table
ALTER TABLE event
ADD COLUMN service_id VARCHAR(32),
ADD COLUMN live BOOLEAN;

--rollback ALTER TABLE event DROP COLUMN service_id, DROP column live;
