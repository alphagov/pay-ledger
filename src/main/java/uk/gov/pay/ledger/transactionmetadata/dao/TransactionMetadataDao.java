package uk.gov.pay.ledger.transactionmetadata.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.util.List;
import java.util.function.BiConsumer;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionMetadataDao {

    private static final String FIND_METADATA_KEYS = "SELECT distinct mk.key FROM transaction t, transaction_metadata tm, metadata_key mk " +
            " WHERE tm.transaction_id = t.id" +
            "  AND tm.metadata_key_id = mk.id" +
            "  AND transaction_details??'external_metadata'" +
            " :searchExtraFields ";

    private static final String FIND_METADATA_KEYS_FOR_TX_SEARCH_INCLUDING_METADATA_VALUE = "SELECT distinct mk.key from metadata_key mk, transaction_metadata tm2 " +
            " WHERE tm2.metadata_key_id = mk.id" +
            "  AND tm2.transaction_id in " +
            "        (SELECT tm.transaction_id " +
            "           FROM transaction t, transaction_metadata tm " +
            "          WHERE tm.transaction_id = t.id " +
            "          AND lower(tm.value) = lower(:metadata_value)" +
            "          :searchExtraFields)";

    private static final String INSERT_STRING = "INSERT INTO transaction_metadata(transaction_id, metadata_key_id) " +
            "SELECT :transactionId, (select id from metadata_key where key = :key) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM transaction_metadata " +
            "    WHERE metadata_key_id in (select id from metadata_key where key = :key ) " +
            "      and transaction_id = :transactionId)";

    private static final String UPSERT_STRING = "INSERT INTO transaction_metadata(transaction_id, metadata_key_id, value) " +
            "VALUES(:transactionId, (select id from metadata_key where key = :key ), :value) " +
            "ON CONFLICT ON CONSTRAINT  transaction_id_and_metadata_key_id_key DO UPDATE SET "+
            "value = EXCLUDED.value WHERE transaction_metadata.transaction_id = EXCLUDED.transaction_id AND " +
            "transaction_metadata.metadata_key_id = EXCLUDED.metadata_key_id";

    private final Jdbi jdbi;

    @Inject
    public TransactionMetadataDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void upsert(Long transactionId, String key, String value) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                        .bind("transactionId", transactionId)
                        .bind("key", key)
                        .bind("value", value)
                        .execute()
        );
    }

    public List<String> findMetadataKeysForTransactions(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {

            String searchClauseTemplate = String.join(" AND ", searchParams.getFilterTemplates());

            if (StringUtils.isNotBlank(searchClauseTemplate)) {
                searchClauseTemplate = "AND " + searchClauseTemplate;
            }

            String queryString = isNotBlank(searchParams.getMetadataValue()) ?
                    FIND_METADATA_KEYS_FOR_TX_SEARCH_INCLUDING_METADATA_VALUE : FIND_METADATA_KEYS;

            queryString = queryString.replace(":searchExtraFields", searchClauseTemplate);

            Query query = handle.createQuery(queryString);
            searchParams.getQueryMap().forEach(bindSearchParameter(query));

            return query
                    .mapTo(String.class)
                    .list();
        });
    }

    private BiConsumer<String, Object> bindSearchParameter(Query query) {
        return (searchKey, searchValue) -> {
            if (searchValue instanceof List<?>) {
                query.bindList(searchKey, ((List<?>) searchValue));
            } else {
                query.bind(searchKey, searchValue);
            }
        };
    }
}