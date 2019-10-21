--liquibase formatted sql

--changeset uk.gov.pay:index_event_date runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS event_date_idx ON event USING btree(event_date);