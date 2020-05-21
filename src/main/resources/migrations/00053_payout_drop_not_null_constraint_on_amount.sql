--liquibase formatted sql

--changeset uk.gov.pay:payout_drop_not_null_constraint_on_amount
ALTER TABLE payout alter amount drop not null ;

--rollback ALTER TABLE payout ALTER COLUMN amount SET NOT NULL;

