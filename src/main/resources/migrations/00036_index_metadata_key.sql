--liquibase formatted sql

--changeset uk.gov.pay:index_metadata_key runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS metadata_key_idx on metadata_key(key)
--rollback drop index CONCURRENTLY metadata_key_idx;