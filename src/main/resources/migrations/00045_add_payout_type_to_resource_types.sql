--liquibase formatted sql

--changeset uk.gov.pay:insert_payout_resourceTypes
INSERT INTO resource_type (name) VALUES ('payout');
--rollback delete from resource_type where name = 'payout'