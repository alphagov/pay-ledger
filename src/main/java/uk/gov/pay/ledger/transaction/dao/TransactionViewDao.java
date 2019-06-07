package uk.gov.pay.ledger.transaction.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.TransactionView;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionViewMapper;

import javax.inject.Inject;
import java.util.List;

public class TransactionViewDao {
    private final Jdbi jdbi;

    //todo: order results by transaction date
    private static final String QUERY_STRING = "SELECT * FROM transaction t " +
            "WHERE t.gateway_account_id = :gatewayAccountExternalId " +
            ":searchExtraFields " +
            "ORDER BY t.id DESC OFFSET :offset LIMIT :limit";

    @Inject
    public TransactionViewDao(Jdbi jdbi) {
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

}
