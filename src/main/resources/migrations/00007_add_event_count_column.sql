--liquibase formatted sql

--changeset uk.gov.pay:add_event_count_column

ALTER TABLE transaction ADD COLUMN event_count INTEGER;