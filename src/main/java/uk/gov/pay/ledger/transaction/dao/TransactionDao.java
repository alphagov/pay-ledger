package uk.gov.pay.ledger.transaction.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.transaction.dao.mapper.TransactionMapper;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionDao {
    private static final String SEARCH_CLAUSE_TRANSACTION_WITH_PAYOUT = "AND po.gateway_account_id = :gatewayAccountId ";
    private static final String SEARCH_CLAUSE_TRANSACTIONS_WITH_PAYOUT = "AND po.gateway_account_id IN (<account_id>) ";

    private static final String FIND_TRANSACTION_BY_EXTERNAL_ID =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            "WHERE t.external_id = :externalId " +
            "AND (:gatewayAccountId is NULL OR t.gateway_account_id = :gatewayAccountId)";

    private static final String FIND_TRANSACTION_BY_EXTERNAL_ID_AND_GATEWAY_ACCOUNT_ID =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            "AND po.gateway_account_id = :gatewayAccountId " +
            "WHERE t.external_id = :externalId " +
            "AND t.gateway_account_id = :gatewayAccountId " +
            "AND (:transactionType::transaction_type is NULL OR type = :transactionType::transaction_type) " +
            "AND (:parentExternalId is NULL OR t.parent_external_id = :parentExternalId)";

    private static final String FIND_TRANSACTIONS_BY_EXTERNAL_OR_PARENT_ID_AND_GATEWAY_ACCOUNT_ID =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            "AND po.gateway_account_id = :gatewayAccountId " +
            "WHERE (t.external_id = :externalId or t.parent_external_id = :externalId) " +
            "AND t.gateway_account_id = :gatewayAccountId";

    private static final String FIND_TRANSACTIONS_BY_PARENT_EXT_ID_AND_GATEWAY_ACCOUNT_ID =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            "WHERE t.parent_external_id = :parentExternalId " +
            "AND t.gateway_account_id = :gatewayAccountId";

    private static final String FIND_TRANSACTIONS_BY_PARENT_EXT_ID =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            "WHERE t.parent_external_id = :parentExternalId";

    private static final String SEARCH_TRANSACTIONS =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            ":searchExtraFields " +
            "ORDER BY t.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String SEARCH_TRANSACTIONS_CURSOR =
            "SELECT t.*, po.paid_out_date AS paid_out_date FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            ":searchExtraFields " +
            ":cursorFields " +
            "ORDER BY t.created_date DESC, t.id DESC LIMIT :limit";

    private static final String COUNT_TRANSACTIONS = "SELECT count(*) " +
            "FROM transaction t " +
            ":searchExtraFields ";

    private static final String COUNT_TRANSACTIONS_WITH_PAIDOUT_DATE = "SELECT count(*) " +
            "FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            ":searchExtraFields ";

    private static final String COUNT_TRANSACTIONS_WITH_LIMIT = "SELECT count(*) FROM (SELECT t.id " +
            "FROM transaction t " +
            " :searchExtraFields " +
            " OFFSET 0 LIMIT :limit" +
            ") txs";

    private static final String COUNT_TRANSACTIONS_WITH_LIMIT_AND_PAIDOUT_DATE = "SELECT count(*) FROM (SELECT t.id " +
            "FROM transaction t " +
            "LEFT OUTER JOIN payout po on " +
            "t.gateway_payout_id = po.gateway_payout_id " +
            ":payoutJoinOnGatewayIdField " +
            " :searchExtraFields " +
            " OFFSET 0 LIMIT :limit" +
            ") txs";

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
                    "live, " +
                    "moto, " +
                    "gateway_transaction_id, " +
                    "source, " +
                    "gateway_payout_id" +
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
                    ":live, " +
                    ":moto, " +
                    ":gatewayTransactionId, " +
                    ":source::source, " +
                    ":gatewayPayoutId" +
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
                    "live = EXCLUDED.live, " +
                    "moto = EXCLUDED.moto, " +
                    "gateway_transaction_id = EXCLUDED.gateway_transaction_id, " +
                    "source = EXCLUDED.source, " +
                    "gateway_payout_id = EXCLUDED.gateway_payout_id " +
                    "WHERE EXCLUDED.event_count >= transaction.event_count;";

    private static final String GET_SOURCE_TYPE_ENUM_VALUES =
            "SELECT " +
                    "pg_enum.enumlabel " +
                    "FROM " +
                    "pg_type " +
                    "JOIN " +
                    "pg_enum ON pg_enum.enumtypid = pg_type.oid " +
                    "WHERE pg_type.typname = 'source';";


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
        return findTransactionByExternalIdAndGatewayAccountId(externalId, null);
    }

    public Optional<TransactionEntity> findTransactionByExternalIdAndGatewayAccountId(String externalId, String gatewayAccountId) {
        String query = FIND_TRANSACTION_BY_EXTERNAL_ID
                .replace(":payoutJoinOnGatewayIdField",
                        isBlank(gatewayAccountId)
                                ? SEARCH_CLAUSE_TRANSACTION_WITH_PAYOUT : "");
        return jdbi.withHandle(handle ->
                handle.createQuery(query)
                        .bind("externalId", externalId)
                        .bind("gatewayAccountId", gatewayAccountId)
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
        String query = FIND_TRANSACTIONS_BY_PARENT_EXT_ID_AND_GATEWAY_ACCOUNT_ID
                .replace(":payoutJoinOnGatewayIdField",
                        isBlank(gatewayAccountId)
                                ? SEARCH_CLAUSE_TRANSACTION_WITH_PAYOUT : "");
        return jdbi.withHandle(handle ->
                handle.createQuery(query)
                        .bind("parentExternalId", parentExternalId)
                        .bind("gatewayAccountId", gatewayAccountId)
                        .map(new TransactionMapper())
                        .stream().collect(Collectors.toList())
        );
    }

    public List<TransactionEntity> findTransactionByParentId(String parentExternalId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_TRANSACTIONS_BY_PARENT_EXT_ID)
                    .bind("parentExternalId", parentExternalId)
                    .map(new TransactionMapper())
                    .stream().collect(Collectors.toList())
        );
    }

    public List<TransactionEntity> searchTransactions(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams, SEARCH_TRANSACTIONS));
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
            Query query = handle.createQuery(createSearchTemplate(searchParams,
                    (isNotBlank(searchParams.getFromSettledDate()) || isNotBlank(searchParams.getToSettledDate())) ?
                            COUNT_TRANSACTIONS_WITH_PAIDOUT_DATE : COUNT_TRANSACTIONS));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            return query
                    .mapTo(Long.class)
                    .one();
        });
    }

    public Long getTotalWithLimitForSearch(TransactionSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams,
                    (isNotBlank(searchParams.getFromSettledDate()) || isNotBlank(searchParams.getToSettledDate())) ?
                            COUNT_TRANSACTIONS_WITH_LIMIT_AND_PAIDOUT_DATE : COUNT_TRANSACTIONS_WITH_LIMIT));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("limit", searchParams.getLimitTotalSize());

            return query
                    .mapTo(Long.class)
                    .one();
        });
    }

    public List<TransactionEntity> cursorTransactionSearch(TransactionSearchParams searchParams, ZonedDateTime startingAfterCreatedDate, Long startingAfterId) {
        Long cursorPageSize = searchParams.getDisplaySize();
        String cursorTemplate = "";
        String searchTemplate = createSearchTemplate(searchParams, SEARCH_TRANSACTIONS_CURSOR);

        if (startingAfterCreatedDate != null && startingAfterId != null) {
            cursorTemplate = searchParams.getQueryMap().isEmpty() ? "WHERE " : "AND ";
            cursorTemplate += "t.created_date <= :startingAfterCreatedDate AND NOT (t.created_date = :startingAfterCreatedDate AND t.id >= :startingAfterId) ";
        }

        searchTemplate = searchTemplate.replace(":cursorFields", cursorTemplate);

        String finalSearchTemplate = searchTemplate;
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(finalSearchTemplate);
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("startingAfterCreatedDate", startingAfterCreatedDate);
            query.bind("startingAfterId", startingAfterId);
            query.bind("limit", cursorPageSize);

            return query.map(new TransactionMapper()).list();
        });
    }

    private String createSearchTemplate(TransactionSearchParams searchParams, String baseQueryString) {
        String searchClauseTemplate = String.join(" AND ", searchParams.getFilterTemplates());
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?
                "WHERE " + searchClauseTemplate :
                "";
        baseQueryString = baseQueryString
                .replace(":payoutJoinOnGatewayIdField",
                        (searchParams.getAccountIds() != null && !searchParams.getAccountIds().isEmpty())
                        ? SEARCH_CLAUSE_TRANSACTIONS_WITH_PAYOUT : "");

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

    public List<String> getSourceTypeValues() {
        return jdbi.withHandle(handle -> handle.createQuery(GET_SOURCE_TYPE_ENUM_VALUES)
                .mapTo(String.class)
                .collect(Collectors.toList()));
    }
}
