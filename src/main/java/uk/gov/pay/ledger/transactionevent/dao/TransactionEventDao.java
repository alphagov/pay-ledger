package uk.gov.pay.ledger.transactionevent.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.customizer.Bind;
import uk.gov.pay.ledger.transactionevent.dao.mapper.TransactionEventEntityMapper;
import uk.gov.pay.ledger.transactionevent.model.TransactionEventEntity;

import java.util.List;

public class TransactionEventDao {

    private static final String TRANSACTION_EVENTS_QUERY = "SELECT t.external_id, t.type, t.amount," +
            "       event_date, event_type, event_data" +
            "  FROM transaction t, event e " +
            " WHERE (t.external_id = :externalId" +
            "         or t.parent_external_id = :externalId )" +
            "  AND t.external_id = e.resource_external_id" +
            "  AND t.gateway_account_id = :gatewayAccountId" +
            " ORDER BY e.event_date asc";

    private final Jdbi jdbi;

    @Inject
    public TransactionEventDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    List<TransactionEventEntity> getTransactionEventsByExternalIdAndGatewayAccountId(@Bind("externalId") String externalId,
                                                                                     @Bind("gatewayAccountId") String gatewayAccountId) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(TRANSACTION_EVENTS_QUERY)
                    .bind("externalId", externalId)
                    .bind("gatewayAccountId", gatewayAccountId);

            return query
                    .map(new TransactionEventEntityMapper())
                    .list();
        });
    }
}
