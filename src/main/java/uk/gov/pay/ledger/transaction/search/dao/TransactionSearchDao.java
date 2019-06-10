package uk.gov.pay.ledger.transaction.search.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.search.mapper.TransactionViewMapper;

import javax.inject.Inject;
import java.util.List;

public class TransactionSearchDao {
    private final Jdbi jdbi;

    //todo: order results by transaction date
    private static final String QUERY_STRING = "SELECT * FROM transaction t " +
            "WHERE t.gateway_account_id = :gatewayAccountExternalId " +
            ":searchExtraFields " +
            "ORDER BY t.id DESC OFFSET :offset LIMIT :limit";

    private static final String COUNT_QUERY_STRING = "SELECT count(t.id) " +
            "FROM transaction t " +
            "WHERE t.gateway_account_id = :gatewayAccountExternalId " +
            ":searchExtraFields ";

    @Inject
    public TransactionSearchDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<TransactionView> searchTransactionView(TransactionSearchParams searchParams) {
        String searchExtraFields = searchParams.generateQuery();
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(QUERY_STRING.replace(":searchExtraFields", searchExtraFields));
            searchParams.getQueryMap().forEach(query::bind);
            return query
                    .map(new TransactionViewMapper())
                    .list();
        });
    }

    public Long getTotalForSearch(TransactionSearchParams searchParams) {
        String searchExtraFields = searchParams.generateQuery();
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(COUNT_QUERY_STRING.replace(":searchExtraFields", searchExtraFields));
            searchParams.getQueryMap().forEach(query::bind);
            return query
                    .mapTo(Long.class)
                    .findOnly();
        });
    }
}
