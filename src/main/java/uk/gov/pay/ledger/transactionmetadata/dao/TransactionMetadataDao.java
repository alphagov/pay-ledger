package uk.gov.pay.ledger.transactionmetadata.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.customizer.Bind;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.util.List;
import java.util.function.BiConsumer;

public class TransactionMetadataDao {

    private static final String FIND_METADATA_KEYS = "SELECT distinct mk.key FROM transaction t, transaction_metadata tm, metadata_key mk " +
            " WHERE tm.transaction_id = t.id" +
            "  AND tm.metadata_key_id = mk.id" +
            "  AND transaction_details??'external_metadata'" +
            " :searchExtraFields ";

    private static final String INSERT_STRING = "INSERT INTO transaction_metadata(transaction_id, metadata_key_id) " +
            "SELECT :transactionId, (select id from metadata_key where key = :key) " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 " +
            "    FROM transaction_metadata " +
            "    WHERE metadata_key_id in (select id from metadata_key where key = :key ) " +
            "      and transaction_id = :transactionId)";

    private final Jdbi jdbi;

    @Inject
    public TransactionMetadataDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void insertIfNotExist(Long transactionId,
                                 String key) {
        jdbi.withHandle(handle ->
                handle.createUpdate(INSERT_STRING)
                        .bind("transactionId", transactionId)
                        .bind("key", key)
                        .execute()
        );
    }

    public List<String> findMetadataKeysForTransactions(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {

            String searchClauseTemplate = String.join(" AND ", searchParams.getFilterTemplates());

            if (StringUtils.isNotBlank(searchClauseTemplate)) {
                searchClauseTemplate = "AND " + searchClauseTemplate;
            }

            String queryString = FIND_METADATA_KEYS.replace(":searchExtraFields", searchClauseTemplate);
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