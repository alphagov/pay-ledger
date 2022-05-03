--liquibase formatted sql

--changeset uk.gov.pay:insert_payment_instrument_resourceTypes
INSERT INTO resource_type (name) VALUES ('payment_instrument');
--rollback delete from resource_type where name = 'payment_instrument'