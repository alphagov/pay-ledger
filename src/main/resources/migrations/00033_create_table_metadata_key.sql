--liquibase formatted sql

--changeset uk.gov.pay:create_table_metadata_key
CREATE TABLE metadata_key (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(50) NOT NULL
);
--rollback drop table metadata_key;
