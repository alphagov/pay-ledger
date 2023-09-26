package uk.gov.pay.ledger.expungeorredact.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.ZonedDateTime;

public interface TransactionRedactionInfoDao {

    @SqlUpdate("INSERT INTO transaction_redaction_info(last_processed_transaction_created_date) " +
            "VALUES (:createdDateOfLastProcessedTransaction)")
    void insert(@Bind("createdDateOfLastProcessedTransaction") ZonedDateTime createdDateOfLastProcessedTransaction);

    @SqlQuery("select max(last_processed_transaction_created_date) from transaction_redaction_info")
    ZonedDateTime getCreatedDateOfLastProcessedTransaction();

    @SqlUpdate("UPDATE transaction_redaction_info " +
            " SET last_processed_transaction_created_date = :createdDateOfLastProcessedTransaction")
    void update(@Bind("createdDateOfLastProcessedTransaction") ZonedDateTime createdDateOfLastProcessedTransaction);

}
