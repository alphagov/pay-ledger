package uk.gov.pay.ledger.transaction.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionMapper;
import uk.gov.pay.ledger.transaction.model.Transaction;

import java.util.List;

@RegisterRowMapper(TransactionMapper.class)
public interface TransactionDao {

    @SqlQuery("SELECT * FROM transaction WHERE gateway_account_id = :gatewayAccountId")
    List<Transaction> getByGatewayAccountId(@Bind("gatewayAccountId") String gatewayAccountId);
}
