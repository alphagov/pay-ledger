--liquibase formatted sql

--changeset uk.gov.pay:update_payment_details_event_name

UPDATE event SET event_type = 'PAYMENT_DETAILS_ENTERED' WHERE event_type = 'PAYMENT_DETAILS_EVENT';
