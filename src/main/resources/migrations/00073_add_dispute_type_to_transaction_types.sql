--liquibase formatted sql

--changeset runInTransaction:false uk.gov.pay:add_dispute_type_to_transaction_type_enum

ALTER type transaction_type ADD VALUE 'DISPUTE';