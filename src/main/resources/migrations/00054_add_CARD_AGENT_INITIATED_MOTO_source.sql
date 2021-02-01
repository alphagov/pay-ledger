--liquibase formatted sql

--changeset uk.gov.pay:add_CARD_AGENT_INITIATED_MOTO_source runInTransaction:false
ALTER TYPE source ADD VALUE 'CARD_AGENT_INITIATED_MOTO' BEFORE 'CARD_EXTERNAL_TELEPHONE';
