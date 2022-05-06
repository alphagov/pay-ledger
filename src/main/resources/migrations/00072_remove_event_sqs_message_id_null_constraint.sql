--liquibase formatted sql

--changeset uk.gov.pay:remove_event_sqs_message_id_not_null_constraint
ALTER TABLE event ALTER COLUMN sqs_message_id DROP NOT NULL;