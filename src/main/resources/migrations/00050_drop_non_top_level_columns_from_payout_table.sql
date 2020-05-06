--liquibase formatted sql

--changeset uk.gov.pay:drop_non_top_level_columns_from_payout
ALTER TABLE payout DROP COLUMN statement_descriptor;
ALTER TABLE payout DROP COLUMN type;

--rollback ALTER TABLE payout ADD COLUMN statement_descriptor VARCHAR(255)
--rollback ALTER TABLE payout ADD COLUMN type VARCHAR(26)

