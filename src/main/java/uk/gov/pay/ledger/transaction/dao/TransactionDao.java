package uk.gov.pay.ledger.transaction.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionMapper;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionWithParentMapper;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TransactionDao {

    private static final String FIND_TRANSACTION_BY_EXTERNAL_ID = "SELECT * FROM transaction " +
            "WHERE external_id = :externalId";

    private static final String FIND_TRANSACTION_BY_EXTERNAL_ID_AND_GATEWAY_ACCOUNT_ID = "SELECT * FROM transaction " +
            "WHERE external_id = :externalId " +
            "AND gateway_account_id = :gatewayAccountId " +
            "AND (:transactionType::transaction_type is NULL OR type = :transactionType::transaction_type) " +
            "AND (:parentExternalId is NULL OR parent_external_id = :parentExternalId)";

    private static final String FIND_TRANSACTIONS_BY_EXTERNAL_OR_PARENT_ID_AND_GATEWAY_ACCOUNT_ID = "SELECT * FROM transaction " +
            "WHERE (external_id = :externalId or parent_external_id = :externalId) " +
            "  AND gateway_account_id = :gatewayAccountId";

    private static final String FIND_TRANSACTIONS_BY_PARENT_EXT_ID_AND_GATEWAY_ACCOUNT_ID = "SELECT * FROM transaction " +
            " WHERE parent_external_id = :parentExternalId" +
            "   AND gateway_account_id = :gatewayAccountId";

    private static final String SEARCH_TRANSACTIONS = "SELECT * FROM transaction t " +
            ":searchExtraFields " +
            "ORDER BY t.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String COUNT_TRANSACTIONS = "SELECT count(1) " +
            "FROM transaction t " +
            ":searchExtraFields ";

    private static final String SEARCH_TRANSACTIONS_WITH_PARENT_TRANSACTION = "select " +
            " t.*," +
            " parent.id as parent_id, " +
            " parent.gateway_account_id as parent_gateway_account_id, " +
            " parent.external_id as parent_external_id, " +
            " parent.parent_external_id as parent_parent_external_id," +
            " parent.amount as parent_amount, " +
            " parent.reference as parent_reference," +
            " parent.description as parent_description, " +
            " parent.state as parent_state," +
            " parent.email as parent_email," +
            " parent.cardholder_name as parent_cardholder_name," +
            " parent.created_date as parent_created_date, " +
            " parent.transaction_details as parent_transaction_details," +
            " parent.event_count as parent_event_count," +
            " parent.card_brand as parent_card_brand," +
            " parent.last_digits_card_number as parent_last_digits_card_number," +
            " parent.first_digits_card_number as parent_first_digits_card_number, " +
            " parent.net_amount as parent_net_amount, " +
            " parent.total_amount as parent_total_amount," +
            " parent.refund_status as parent_refund_status," +
            " parent.refund_amount_refunded as parent_refund_amount_refunded," +
            " parent.refund_amount_available as parent_refund_amount_available, " +
            " parent.fee as parent_fee," +
            " parent.type as parent_type " +
            " from transaction t left outer join transaction parent " +
            " on t.parent_external_id = parent.external_id " +
            ":searchExtraFields " +
            "ORDER BY t.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String COUNT_TRANSACTIONS_WITH_PARENT_TRANSACTION = "select " +
            " count(1) " +
            " from transaction t left outer join transaction parent " +
            " on t.parent_external_id = parent.external_id " +
            ":searchExtraFields ";

    private static final String UPSERT_STRING =
            "INSERT INTO transaction(" +
                    "external_id," +
                    "parent_external_id," +
                    "gateway_account_id," +
                    "amount," +
                    "description," +
                    "reference,state," +
                    "email," +
                    "cardholder_name," +
                    "created_date," +
                    "transaction_details," +
                    "event_count," +
                    "card_brand, " +
                    "last_digits_card_number," +
                    "first_digits_card_number," +
                    "net_amount," +
                    "total_amount," +
                    "fee,type," +
                    "refund_amount_available," +
                    "refund_amount_refunded, " +
                    "refund_status, " +
                    "live" +
                    ") " +
                    "VALUES (" +
                    ":externalId," +
                    ":parentExternalId," +
                    ":gatewayAccountId," +
                    ":amount," +
                    ":description," +
                    ":reference," +
                    ":state," +
                    ":email," +
                    ":cardholderName," +
                    ":createdDate," +
                    "CAST(:transactionDetails as jsonb)," +
                    ":eventCount," +
                    ":cardBrand," +
                    ":lastDigitsCardNumber," +
                    ":firstDigitsCardNumber," +
                    ":netAmount," +
                    ":totalAmount," +
                    ":fee," +
                    ":transactionType::transaction_type," +
                    ":refundAmountAvailable," +
                    ":refundAmountRefunded," +
                    ":refundStatus," +
                    ":live" +
            ") " +
            "ON CONFLICT (external_id) " +
            "DO UPDATE SET " +
                "external_id = EXCLUDED.external_id," +
                "parent_external_id = EXCLUDED.parent_external_id," +
                "gateway_account_id = EXCLUDED.gateway_account_id," +
                "amount = EXCLUDED.amount," +
                "description = EXCLUDED.description," +
                "reference = EXCLUDED.reference," +
                "state = EXCLUDED.state," +
                "email = EXCLUDED.email," +
                "cardholder_name = EXCLUDED.cardholder_name," +
                "created_date = EXCLUDED.created_date," +
                "transaction_details = EXCLUDED.transaction_details," +
                "event_count = EXCLUDED.event_count," +
                "card_brand = EXCLUDED.card_brand," +
                "last_digits_card_number = EXCLUDED.last_digits_card_number," +
                "first_digits_card_number = EXCLUDED.first_digits_card_number," +
                "net_amount = EXCLUDED.net_amount," +
                "total_amount = EXCLUDED.total_amount," +
                "fee = EXCLUDED.fee," +
                "type = EXCLUDED.type, " +
                "refund_amount_available = EXCLUDED.refund_amount_available, " +
                "refund_amount_refunded = EXCLUDED.refund_amount_refunded, " +
                "refund_status = EXCLUDED.refund_status, " +
                "live = EXCLUDED.live " +
            "WHERE EXCLUDED.event_count >= transaction.event_count;";

    private final Jdbi jdbi;

    @Inject
    public TransactionDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<TransactionEntity> findTransaction(String externalId, String gatewayAccountId, TransactionType transactionType, String parentTransactionExternalId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTION_BY_EXTERNAL_ID_AND_GATEWAY_ACCOUNT_ID)
                        .bind("externalId", externalId)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .bind("transactionType", transactionType)
                        .bind("parentExternalId", parentTransactionExternalId)
                        .map(new TransactionMapper())
                        .findFirst());
    }

    public Optional<TransactionEntity> findTransactionByExternalId(String externalId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTION_BY_EXTERNAL_ID)
                        .bind("externalId", externalId)
                        .map(new TransactionMapper())
                        .findFirst());
    }

    public List<TransactionEntity> findTransactionByExternalOrParentIdAndGatewayAccountId(String externalId, String gatewayAccountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTIONS_BY_EXTERNAL_OR_PARENT_ID_AND_GATEWAY_ACCOUNT_ID)
                        .bind("externalId", externalId)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .map(new TransactionMapper())
                        .stream().collect(Collectors.toList())
        );
    }

    public List<TransactionEntity> findTransactionByParentIdAndGatewayAccountId(String parentExternalId, String gatewayAccountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTIONS_BY_PARENT_EXT_ID_AND_GATEWAY_ACCOUNT_ID)
                        .bind("parentExternalId", parentExternalId)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .map(new TransactionMapper())
                        .stream().collect(Collectors.toList())
        );
    }

    public List<TransactionEntity> searchTransactions(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), SEARCH_TRANSACTIONS));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("offset", searchParams.getOffset());
            query.bind("limit", searchParams.getDisplaySize());
            return query
                    .map(new TransactionMapper())
                    .list();
        });
    }

    public Long getTotalForSearch(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), COUNT_TRANSACTIONS));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            return query
                    .mapTo(Long.class)
                    .findOnly();
        });
    }

    public List<TransactionEntity> searchTransactionsAndParent(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplatesWithParentTransactionSearch(), SEARCH_TRANSACTIONS_WITH_PARENT_TRANSACTION));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("offset", searchParams.getOffset());
            query.bind("limit", searchParams.getDisplaySize());

            return query
                    .map(new TransactionWithParentMapper())
                    .list();
        });
    }

    public Long getTotalForSearchTransactionAndParent(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplatesWithParentTransactionSearch(), COUNT_TRANSACTIONS_WITH_PARENT_TRANSACTION));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            return query
                    .mapTo(Long.class)
                    .findOnly();
        });
    }

    private String createSearchTemplate(
            List<String> filterTemplates,
            String baseQueryString
    ) {
        String searchClauseTemplate = String.join(" AND ", filterTemplates);
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?
                "WHERE " + searchClauseTemplate :
                "";

        return baseQueryString.replace(
                ":searchExtraFields",
                searchClauseTemplate);
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

    public void upsert(TransactionEntity transaction) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_STRING)
                .bindBean(transaction)
                .execute());
    }
}
