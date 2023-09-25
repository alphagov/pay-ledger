--liquibase formatted sql

--changeset uk.gov.pay:create_table_transaction_redaction_info
CREATE TABLE transaction_redaction_info (
    last_processed_transaction_created_date TIMESTAMP WITH TIME ZONE
);

--rollback drop table transaction_redaction_info;
