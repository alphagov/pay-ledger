package uk.gov.pay.ledger.transaction.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionMapper;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.util.List;
import java.util.Optional;

public class TransactionDao {

    private static final String FIND_TRANSACTION_BY_EXTERNAL_ID = "SELECT * FROM transaction " +
            "WHERE external_id = :externalId";

    private static final String SEARCH_QUERY_STRING = "SELECT * FROM transaction t " +
            "WHERE t.gateway_account_id = :account_id " +
            ":searchExtraFields " +
            "ORDER BY t.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String SEARCH_COUNT_QUERY_STRING = "SELECT count(t.id) " +
            "FROM transaction t " +
            "WHERE t.gateway_account_id = :account_id " +
            ":searchExtraFields ";

    private static final String UPSERT_STRING =
            "INSERT INTO transaction(" +
                "external_id,gateway_account_id,amount,description,reference,state,email,cardholder_name," +
                "external_metadata,created_date,transaction_details,event_count,card_brand, " +
                    "last_digits_card_number,first_digits_card_number" +
            ") " +
            "VALUES (" +
                ":externalId,:gatewayAccountId,:amount,:description,:reference,:state,:email,:cardholderName," +
                "CAST(:externalMetadata as jsonb),:createdDate,CAST(:transactionDetails as jsonb), :eventCount," +
                    ":cardBrand,:lastDigitsCardNumber,:firstDigitsCardNumber" +
            ")" +
            "ON CONFLICT (external_id) " +
            "DO UPDATE SET " +
                "external_id = EXCLUDED.external_id," +
                "gateway_account_id = EXCLUDED.gateway_account_id," +
                "amount = EXCLUDED.amount," +
                "description = EXCLUDED.description," +
                "reference = EXCLUDED.reference," +
                "state = EXCLUDED.state," +
                "email = EXCLUDED.email," +
                "cardholder_name = EXCLUDED.cardholder_name," +
                "external_metadata = EXCLUDED.external_metadata," +
                "created_date = EXCLUDED.created_date," +
                "transaction_details = EXCLUDED.transaction_details," +
                "event_count = EXCLUDED.event_count," +
                "card_brand = EXCLUDED.card_brand," +
                "last_digits_card_number = EXCLUDED.last_digits_card_number," +
                "first_digits_card_number = EXCLUDED.first_digits_card_number " +
            "WHERE EXCLUDED.event_count > transaction.event_count;";

    private final Jdbi jdbi;

    @Inject
    public TransactionDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<TransactionEntity> findTransactionByExternalId(String externalId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTION_BY_EXTERNAL_ID)
                        .bind("externalId", externalId)
                        .map(new TransactionMapper())
                        .findFirst());
    }

    public List<TransactionEntity> searchTransactions(TransactionSearchParams searchParams) {
        String searchExtraFields = searchParams.generateQuery();
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(SEARCH_QUERY_STRING.replace(":searchExtraFields", searchExtraFields));
            searchParams.getQueryMap().forEach(query::bind);
            return query
                    .map(new TransactionMapper())
                    .list();
        });
    }

    public Long getTotalForSearch(TransactionSearchParams searchParams) {
        String searchExtraFields = searchParams.generateQuery();
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(SEARCH_COUNT_QUERY_STRING.replace(":searchExtraFields", searchExtraFields));
            searchParams.getQueryMap().forEach(query::bind);
            return query
                    .mapTo(Long.class)
                    .findOnly();
        });
    }

    public void upsert(TransactionEntity transaction) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                .bindBean(transaction)
                .execute());
    }
}
