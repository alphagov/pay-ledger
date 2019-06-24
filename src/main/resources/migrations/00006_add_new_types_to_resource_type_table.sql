--liquibase formatted sql

--changeset uk.gov.pay:insert_new_resource_types
INSERT INTO resource_type (name) VALUES ('payment'), ('card_payment');
