--liquibase formatted sql

--changeset uk.gov.pay:build_table_event
CREATE TABLE event
(
    id BIGSERIAL PRIMARY KEY,
    sqs_message_id VARCHAR(255) NOT NULL,
    resource_type_id INT NOT NULL,
    resource_external_id VARCHAR(255),
    event_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'utc') NOT NULL,
    event_type VARCHAR (255),
    event_data jsonb NOT NULL
);
--rollback drop table event;

--changeset uk.gov.pay:add_event_resource_type_id_fk
ALTER TABLE event ADD CONSTRAINT event_resource_type_id_fk FOREIGN KEY (resource_type_id) REFERENCES resource_type (id);
--rollback drop constraint event_resource_type_id_fk;

--changeset uk.gov.pay:add_index_resource_external_id
CREATE INDEX index_resource_external_idx ON event(resource_external_id);
--rollback drop index index_resource_external_idx;
