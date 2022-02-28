--liquibase formatted sql

--changeset uk.gov.pay:insert_dispute_resourceTypes
INSERT INTO resource_type (name) VALUES ('dispute');
--rollback delete from resource_type where name = 'dispute'