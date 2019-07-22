--liquibase formatted sql

--changeset uk.gov.pay:add_parent_resource_external_id_to_event_table

ALTER TABLE event
    ADD COLUMN parent_resource_external_id VARCHAR(255);

CREATE index parent_resource_external_id_idx on event(parent_resource_external_id);
