--liquibase formatted sql

--changeset uk.gov.pay:build event table
CREATE TABLE event (
    id VARCHAR(255) PRIMARY KEY
);
--rollback drop table event;
