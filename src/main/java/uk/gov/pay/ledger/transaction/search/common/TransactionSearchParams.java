package uk.gov.pay.ledger.transaction.search.common;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.common.search.SearchParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.service.payments.commons.validation.DateTimeUtils.fromLocalDateOnlyString;

public class TransactionSearchParams extends SearchParams {

    private static final String GATEWAY_ACCOUNT_EXTERNAL_FIELD = "account_id";
    private static final String CARDHOLDER_NAME_FIELD = "cardholder_name";
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";
    private static final String EMAIL_FIELD = "email";
    private static final String REFERENCE_FIELD = "reference";
    private static final String LAST_DIGITS_CARD_NUMBER_FIELD = "last_digits_card_number";
    private static final String FIRST_DIGITS_CARD_NUMBER_FIELD = "first_digits_card_number";
    private static final String PAYMENT_STATES_FIELD = "payment_states";
    private static final String REFUND_STATES_FIELD = "refund_states";
    private static final String DISPUTE_STATES_FIELD = "dispute_states";
    private static final String CARD_BRAND_FIELD = "card_brand";
    private static final String STATE_FIELD = "state";
    private static final String TRANSACTION_TYPE_FIELD = "transaction_type";
    private static final String GATEWAY_TRANSACTION_ID_FIELD = "gateway_transaction_id";
    private static final String GATEWAY_PAYOUT_ID = "gateway_payout_id";
    private static final String FROM_SETTLED_DATE_FIELD = "from_settled_date";
    private static final String TO_SETTLED_DATE_FIELD = "to_settled_date";
    private static final long DEFAULT_PAGE_NUMBER = 1L;
    private static final long DEFAULT_MAX_DISPLAY_SIZE = 500L;
    private static final Long DEFAULT_LIMIT_TOTAL_SIZE = 10000L;
    private static final String METADATA_VALUE = "metadata_value";

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    int statusVersion;
    private boolean exactReferenceMatch;
    private List<String> accountIds;
    private String email;
    private String reference;
    private String cardHolderName;
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private CommaDelimitedSetParameter paymentStates;
    private String state;
    private CommaDelimitedSetParameter refundStates;
    private CommaDelimitedSetParameter cardBrands;
    private String fromDate;
    private String toDate;
    private TransactionType transactionType;
    private String gatewayPayoutId;
    private String fromSettledDate;
    private String toSettledDate;
    private String metadataValue;
    private Long pageNumber = 1L;

    private Long displaySize = DEFAULT_MAX_DISPLAY_SIZE;
    private boolean limitTotal;
    private Long limitTotalSize = DEFAULT_LIMIT_TOTAL_SIZE;

    private Map<String, Object> queryMap;
    private String gatewayTransactionId;
    private CommaDelimitedSetParameter disputeStates;

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = List.copyOf(accountIds);
    }

    @QueryParam("email")
    @Parameter(example = "test@example.org")
    public void setEmail(String email) {
        this.email = email;
    }

    @Parameter(example = "my-payment-reference")
    @QueryParam("reference")
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Parameter(example = "J Doe")
    @QueryParam("cardholder_name")
    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    @Parameter(example = "7890")
    @QueryParam("last_digits_card_number")
    public void setLastDigitsCardNumber(String lastDigitsCardNumber) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
    }

    @Parameter(example = "123456")
    @QueryParam("first_digits_card_number")
    public void setFirstDigitsCardNumber(String firstDigitsCardNumber) {
        this.firstDigitsCardNumber = firstDigitsCardNumber;
    }

    @Parameter(description = "Comma delimited payment states.", example = "success,error", schema = @Schema(type = "string", implementation = String.class))
    @QueryParam("payment_states")
    public void setPaymentStates(CommaDelimitedSetParameter paymentStates) {
        this.paymentStates = paymentStates;
    }

    @Parameter(description = "Comma delimited refund states.", example = "success,error", schema = @Schema(type = "string", implementation = String.class))
    @QueryParam("refund_states")
    public void setRefundStates(CommaDelimitedSetParameter refundStates) {
        this.refundStates = refundStates;
    }

    @Parameter(description = "Comma delimited card brands.", example = "visa,mastercard", schema = @Schema(type = "string", implementation = String.class))
    @QueryParam("card_brands")
    public void setCardBrands(CommaDelimitedSetParameter cardBrands) {
        this.cardBrands = cardBrands;
    }

    @Parameter(description = "From date of transactions to be searched (this date is inclusive).", example = "\"2015-08-14T12:35:00Z\"")
    @QueryParam("from_date")
    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    @Parameter(description = "To date of transactions to be searched (this date is inclusive).", example = "\"2015-08-14T12:35:00Z\"")
    @QueryParam("to_date")
    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    @Parameter(example = "success")
    @QueryParam("state")
    public void setState(String state) {
        this.state = state;
    }

    @QueryParam(TRANSACTION_TYPE_FIELD)
    @Parameter(example = "PAYMENT")
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    @DefaultValue("2")
    @Parameter(description = "Set to '2' to return failed transaction states FAILED_REJECTED/FAILED_EXPIRED/FAILED_CANCELLED" +
            " mapped to declined/timedout/cancelled external status respectively." +
            "Otherwise these transaction states will all be mapped to `failed` status", schema = @Schema(defaultValue = "2"))
    @QueryParam("status_version")
    public void setStatusVersion(int statusVersion) {
        this.statusVersion = statusVersion;
    }

    @Parameter(example = "po_fj893joishj12lndk")
    @QueryParam("gateway_payout_id")
    public void setGatewayPayoutId(String gatewayPayoutId) {
        this.gatewayPayoutId = gatewayPayoutId;
    }

    @Parameter(description = "From date of transactions settled date to be searched (this date is inclusive).", example = "\"2015-08-14\"")
    @QueryParam("from_settled_date")
    public void setFromSettledDate(String fromSettledDate) {
        this.fromSettledDate = fromSettledDate;
    }

    @Parameter(description = "To date of transactions settled date to be searched (this date is inclusive).", example = "\"2015-08-14\"")
    @QueryParam("to_settled_date")
    public void setToSettledDate(String toSettledDate) {
        this.toSettledDate = toSettledDate;
    }

    @QueryParam("metadata_value")
    @Parameter(example = "metadata-value-1")
    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }

    @QueryParam("page")
    @Parameter(example = "1")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    @Parameter(example = "100", schema = @Schema(defaultValue = "500"))
    @DefaultValue("500")
    @QueryParam("display_size")
    public void setDisplaySize(Long displaySize) {
        this.displaySize = displaySize;
    }

    @Parameter(example = "1000", schema = @Schema(defaultValue = "10000"))
    @DefaultValue("10000")
    @QueryParam("limit_total_size")
    public void setLimitTotalSize(Long limitTotalSize) {
        this.limitTotalSize = limitTotalSize;
    }

    @Parameter(example = "true", description = "Set to 'true' to limit the search counting the total number of transactions to 'limit_total_size' param")
    @DefaultValue("false")
    @QueryParam("limit_total")
    public void setLimitTotal(boolean limitTotal) {
        this.limitTotal = limitTotal;
    }

    @Parameter(description = "Comma delimited dispute states.", example = "won,needs_response", schema = @Schema(type = "string", implementation = String.class))
    @QueryParam("dispute_states")
    public void setDisputeStates(CommaDelimitedSetParameter disputeStates) {
        this.disputeStates = disputeStates;
    }

    public void overrideMaxDisplaySize(Long maxDisplaySize) {
        this.maxDisplaySize = maxDisplaySize;
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        addCommonFilterTemplates(filters);

        if (isNotBlank(email)) {
            filters.add(" lower(t.email) LIKE lower(:" + EMAIL_FIELD + ")");
        }
        if (isNotBlank(reference)) {
            if (exactReferenceMatch) {
                filters.add(" lower(t.reference) = lower(:" + REFERENCE_FIELD + ")");
            } else {
                filters.add(" lower(t.reference) LIKE lower(:" + REFERENCE_FIELD + ")");
            }
        }
        if (isNotBlank(cardHolderName)) {
            filters.add(" lower(t.cardholder_name) LIKE lower(:" + CARDHOLDER_NAME_FIELD + ")");
        }
        if (cardBrands != null && cardBrands.isNotEmpty()) {
            filters.add(" t.card_brand IN (<" + CARD_BRAND_FIELD + ">)");
        }
        if (isNotBlank(lastDigitsCardNumber)) {
            filters.add(" t.last_digits_card_number = :" + LAST_DIGITS_CARD_NUMBER_FIELD);
        }
        if (isNotBlank(gatewayTransactionId)) {
            filters.add(" t.gateway_transaction_id = :" + GATEWAY_TRANSACTION_ID_FIELD);
        }
        if (isNotBlank(fromSettledDate)) {
            filters.add(" po.paid_out_date >= :" + FROM_SETTLED_DATE_FIELD);
        }
        if (isNotBlank(toSettledDate)) {
            filters.add(" po.paid_out_date < :" + TO_SETTLED_DATE_FIELD);
        }
        if (isNotBlank(metadataValue)) {
            filters.add(" lower(tm.value) = lower(:" + METADATA_VALUE + ")");
        }

        return List.copyOf(filters);
    }

    private void addCommonFilterTemplates(List<String> filters) {

        if (accountIds != null && !accountIds.isEmpty()) {
            filters.add(" t.gateway_account_id IN (<" + GATEWAY_ACCOUNT_EXTERNAL_FIELD + ">)");
        }
        if (transactionType != null) {
            filters.add(" t.type = :" + TRANSACTION_TYPE_FIELD + "::transaction_type");
        }
        if (isNotBlank(fromDate)) {
            filters.add(" t.created_date > :" + FROM_DATE_FIELD);
        }
        if (isNotBlank(toDate)) {
            filters.add(" t.created_date < :" + TO_DATE_FIELD);
        }
        if (isSet(paymentStates) || isSet(refundStates) || isSet(disputeStates)) {
            filters.add(createStateFilter());
        }
        if (isNotBlank(state)) {
            filters.add(" t.state IN (<" + STATE_FIELD + ">)");
        }
        if (isNotBlank(firstDigitsCardNumber)) {
            filters.add(" t.first_digits_card_number = :" + FIRST_DIGITS_CARD_NUMBER_FIELD);
        }
        if (isNotBlank(gatewayPayoutId)) {
            filters.add(" t.gateway_payout_id = :" + GATEWAY_PAYOUT_ID);
        }
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (accountIds != null && !accountIds.isEmpty()) {
                queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountIds);
            }

            if (isNotBlank(email)) {
                queryMap.put(EMAIL_FIELD, likeClause(email));
            }
            if (isNotBlank(reference)) {
                if (exactReferenceMatch) {
                    queryMap.put(REFERENCE_FIELD, reference);
                } else {
                    queryMap.put(REFERENCE_FIELD, likeClause(reference));
                }
            }
            if (isNotBlank(cardHolderName)) {
                queryMap.put(CARDHOLDER_NAME_FIELD, likeClause(cardHolderName));
            }
            if (isNotBlank(fromDate)) {
                queryMap.put(FROM_DATE_FIELD, ZonedDateTime.parse(fromDate));
            }
            if (isNotBlank(toDate)) {
                queryMap.put(TO_DATE_FIELD, ZonedDateTime.parse(toDate));
            }
            if (isNotBlank(state)) {
                queryMap.put(STATE_FIELD,
                        getTransactionState(state, statusVersion).stream()
                                .map(TransactionState::name)
                                .collect(Collectors.toList()));
            }
            if (cardBrands != null && cardBrands.isNotEmpty()) {
                queryMap.put(CARD_BRAND_FIELD, cardBrands.getParameters());
            }
            if (isSet(paymentStates)) {
                queryMap.put(PAYMENT_STATES_FIELD, getTransactionStatesForStatuses(paymentStates));
            }
            if (isSet(refundStates)) {
                queryMap.put(REFUND_STATES_FIELD, getTransactionStatesForStatuses(refundStates));
            }
            if (isSet(disputeStates)) {
                queryMap.put(DISPUTE_STATES_FIELD, getTransactionStatesForStatuses(disputeStates));
            }
            if (isNotBlank(lastDigitsCardNumber)) {
                queryMap.put(LAST_DIGITS_CARD_NUMBER_FIELD, lastDigitsCardNumber);
            }
            if (isNotBlank(firstDigitsCardNumber)) {
                queryMap.put(FIRST_DIGITS_CARD_NUMBER_FIELD, firstDigitsCardNumber);
            }
            if (transactionType != null) {
                queryMap.put(TRANSACTION_TYPE_FIELD, transactionType);
            }
            if (gatewayTransactionId != null) {
                queryMap.put(GATEWAY_TRANSACTION_ID_FIELD, gatewayTransactionId);
            }
            if (isNotBlank(gatewayPayoutId)) {
                queryMap.put(GATEWAY_PAYOUT_ID, gatewayPayoutId);
            }
            if (isNotBlank(fromSettledDate)) {
                queryMap.put(FROM_SETTLED_DATE_FIELD, parseFromSettledDate());
            }
            if (isNotBlank(toSettledDate)) {
                queryMap.put(TO_SETTLED_DATE_FIELD, parseToSettledDate());
            }
            if (isNotBlank(metadataValue)) {
                queryMap.put(METADATA_VALUE, metadataValue);
            }
        }
        return queryMap;
    }

    private List<TransactionState> getTransactionState(String status, int statusVersion) {
        return statusVersion == 2 ?
                TransactionState.getStatesForStatus(status) :
                TransactionState.getStatesForOldStatus(status);
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    @Override
    public Long getPageNumber() {
        return pageNumber;
    }

    @Override
    public Long getDisplaySize() {
        if (this.displaySize > this.maxDisplaySize) {
            return this.maxDisplaySize;
        } else {
            return this.displaySize;
        }
    }

    @Override
    public boolean limitTotal() {
        return limitTotal;
    }

    public Long getLimitTotalSize() {
        if (limitTotalSize < displaySize) {
            return DEFAULT_LIMIT_TOTAL_SIZE;
        }
        return this.limitTotalSize;
    }

    public String getFromDate() {
        return fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public int getStatusVersion() {
        return statusVersion;
    }

    public String getFromSettledDate() {
        return fromSettledDate;
    }

    public String getToSettledDate() {
        return toSettledDate;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    @Override
    public String buildQueryParamString(Long forPage) {
        List<String> queries = new ArrayList<>();

        if (accountIds != null && !accountIds.isEmpty()) {
            queries.add(GATEWAY_ACCOUNT_EXTERNAL_FIELD + "=" + String.join(",", accountIds));
        }
        if (isNotBlank(fromDate)) {
            queries.add(FROM_DATE_FIELD + "=" + fromDate);
        }
        if (isNotBlank(toDate)) {
            queries.add(TO_DATE_FIELD + "=" + toDate);
        }

        if (isNotBlank(email)) {
            queries.add(EMAIL_FIELD + "=" + email);
        }
        if (isNotBlank(reference)) {
            queries.add(REFERENCE_FIELD + "=" + reference);
        }
        if (isNotBlank(cardHolderName)) {
            queries.add(CARDHOLDER_NAME_FIELD + "=" + cardHolderName);
        }
        if (isNotBlank(firstDigitsCardNumber)) {
            queries.add(FIRST_DIGITS_CARD_NUMBER_FIELD + "=" + firstDigitsCardNumber);
        }
        if (isNotBlank(lastDigitsCardNumber)) {
            queries.add(LAST_DIGITS_CARD_NUMBER_FIELD + "=" + lastDigitsCardNumber);
        }
        if (isSet(paymentStates)) {
            queries.add(PAYMENT_STATES_FIELD + "=" + paymentStates.getRawString());
        }
        if (refundStates != null && refundStates.isNotEmpty()) {
            queries.add(REFUND_STATES_FIELD + "=" + refundStates.getRawString());
        }
        if (isSet(disputeStates)) {
            queries.add(DISPUTE_STATES_FIELD + "=" + disputeStates.getRawString());
        }
        if (cardBrands != null && cardBrands.isNotEmpty()) {
            queries.add(CARD_BRAND_FIELD + "=" + cardBrands.getRawString());
        }
        if (isNotBlank(state)) {
            queries.add(STATE_FIELD + "=" + state);
        }
        if (transactionType != null) {
            queries.add(TRANSACTION_TYPE_FIELD + "=" + transactionType);
        }
        if (isNotBlank(gatewayPayoutId)) {
            queries.add(GATEWAY_PAYOUT_ID + "=" + gatewayPayoutId);
        }
        if (isNotBlank(fromSettledDate)) {
            queries.add(FROM_SETTLED_DATE_FIELD + "=" + fromSettledDate);
        }
        if (isNotBlank(toSettledDate)) {
            queries.add(TO_SETTLED_DATE_FIELD + "=" + toSettledDate);
        }
        if (isNotBlank(metadataValue)) {
            queries.add(METADATA_VALUE + "=" + metadataValue);
        }
        queries.add("page=" + forPage);
        queries.add("display_size=" + getDisplaySize());


        return String.join("&", queries);
    }

    public Long getOffset() {
        long offset = 0L;

        if (pageNumber != null) {
            offset = (pageNumber - 1) * getDisplaySize();
        }

        return offset;
    }

    private List<String> getTransactionStatesForStatuses(CommaDelimitedSetParameter transactionStates) {
        return transactionStates.getParameters().stream()
                .map(status -> getTransactionState(status, statusVersion))
                .flatMap(List::stream)
                .map(TransactionState::name)
                .collect(Collectors.toList());
    }

    private String createStateFilter() {
        String paymentStateFilter = null;
        String refundStateFilter = null;
        String disputeStatesFilter = null;
        if (isSet(paymentStates)) {
            paymentStateFilter =
                    " (t.state IN (<" + PAYMENT_STATES_FIELD + ">) AND t.type =  'PAYMENT'::transaction_type)";
        }
        if (isSet(refundStates)) {
            refundStateFilter =
                    " (t.state IN (<" + REFUND_STATES_FIELD + ">) AND t.type =  'REFUND'::transaction_type)";
        }

        if (isSet(disputeStates)) {
            disputeStatesFilter =
                    " (t.state IN (<" + DISPUTE_STATES_FIELD + ">) AND t.type =  'DISPUTE'::transaction_type)";
        }

        return "(" + List.of(
                        Optional.ofNullable(paymentStateFilter),
                        Optional.ofNullable(refundStateFilter),
                        Optional.ofNullable(disputeStatesFilter))
                .stream()
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" OR ")) + ")";
    }

    private String likeClause(String rawUserInputText) {
        return "%" + rawUserInputText + "%";
    }

    @DefaultValue("false")
    @Schema(name = "exact_reference_match")
    @Parameter(name = "exact_reference_match", description = "Set to 'true' to search for transactions by exact reference. Otherwise reference is partially matched")
    @QueryParam("exact_reference_match")
    public void setExactReferenceMatch(Boolean exactReferenceMatch) {
        this.exactReferenceMatch = exactReferenceMatch;
    }

    @Parameter(example = "a14f0926-b44d-4160-8184-1b1f66e576ab")
    @QueryParam("gateway_transaction_id")
    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    private ZonedDateTime parseFromSettledDate() {
        return fromLocalDateOnlyString(fromSettledDate).get();
    }

    private ZonedDateTime parseToSettledDate() {
        return fromLocalDateOnlyString(toSettledDate).get().plusDays(1L);
    }
}
