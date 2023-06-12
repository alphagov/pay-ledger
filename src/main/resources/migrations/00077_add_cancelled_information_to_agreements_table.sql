--liquibase formatted sql

--changeset uk.gov.pay:add_cancelled_date_column_to_agreements_table
ALTER TABLE agreement
    ADD COLUMN cancelled_date TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancelled_by_user_email VARCHAR(254);

--rollback drop column cancelled_date, cancelled_by_user_email;
