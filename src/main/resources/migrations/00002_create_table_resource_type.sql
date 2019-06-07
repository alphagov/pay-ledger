--liquibase formatted sql

--changeset uk.gov.pay:create_table_resource_type
create table resource_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE
);
--rollback drop table resource_type;

--changeset uk.gov.pay:insert_resourceTypes
INSERT INTO resource_type (name) VALUES ('agreement'), ('charge'), ('refund'), ('service');
--rollback truncate table resource_type
