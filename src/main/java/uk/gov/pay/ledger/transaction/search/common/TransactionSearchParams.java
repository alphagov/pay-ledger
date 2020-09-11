package uk.gov.pay.ledger.transaction.search.common;

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

    private long maxDisplaySize = DEFAULT_MAX_DISPLAY_SIZE;

    @DefaultValue("2")
    @QueryParam("status_version")
    int statusVersion;
    @DefaultValue("false")
    @QueryParam("exact_reference_match")
    private boolean exactReferenceMatch;
    private List<String> accountIds;
    @QueryParam("email")
    private String email;
    @QueryParam("reference")
    private String reference;
    @QueryParam("cardholder_name")
    private String cardHolderName;
    @QueryParam("last_digits_card_number")
    private String lastDigitsCardNumber;
    @QueryParam("first_digits_card_number")
    private String firstDigitsCardNumber;
    @QueryParam("payment_states")
    private CommaDelimitedSetParameter paymentStates;
    @QueryParam("state")
    private String state;
    @QueryParam("refund_states")
    private CommaDelimitedSetParameter refundStates;
    @QueryParam("card_brands")
    private CommaDelimitedSetParameter cardBrands;
    @QueryParam("from_date")
    private String fromDate;
    @QueryParam("to_date")
    private String toDate;
    @QueryParam(TRANSACTION_TYPE_FIELD)
    private TransactionType transactionType;
    @QueryParam("gateway_payout_id")
    private String gatewayPayoutId;
    @QueryParam("from_settled_date")
    private String fromSettledDate;
    @QueryParam("to_settled_date")
    private String toSettledDate;
    private Long pageNumber = 1L;

    @DefaultValue("500")
    @QueryParam("display_size")
    private Long displaySize = DEFAULT_MAX_DISPLAY_SIZE;

    @DefaultValue("false")
    @QueryParam("limit_total")
    private boolean limitTotal;
    @DefaultValue("10000")
    @QueryParam("limit_total_size")
    private Long limitTotalSize = DEFAULT_LIMIT_TOTAL_SIZE;

    private Map<String, Object> queryMap;
    @QueryParam("gateway_transaction_id")
    private String gatewayTransactionId;

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = List.copyOf(accountIds);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public void setLastDigitsCardNumber(String lastDigitsCardNumber) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
    }

    public void setFirstDigitsCardNumber(String firstDigitsCardNumber) {
        this.firstDigitsCardNumber = firstDigitsCardNumber;
    }

    public void setPaymentStates(CommaDelimitedSetParameter paymentStates) {
        this.paymentStates = paymentStates;
    }

    public void setRefundStates(CommaDelimitedSetParameter refundStates) {
        this.refundStates = refundStates;
    }

    public void setCardBrands(CommaDelimitedSetParameter cardBrands) {
        this.cardBrands = cardBrands;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public void setStatusVersion(int statusVersion) {
        this.statusVersion = statusVersion;
    }

    public void setGatewayPayoutId(String gatewayPayoutId) {
        this.gatewayPayoutId = gatewayPayoutId;
    }

    public void setFromSettledDate(String fromSettledDate) {
        this.fromSettledDate = fromSettledDate;
    }

    public void setToSettledDate(String toSettledDate) {
        this.toSettledDate = toSettledDate;
    }

    @QueryParam("page")
    public void setPageNumber(Long pageNumber) {
        this.pageNumber = Objects.requireNonNullElse(pageNumber, DEFAULT_PAGE_NUMBER);
    }

    public void setDisplaySize(Long displaySize) {
        this.displaySize = displaySize;
    }

    public void setLimitTotalSize(Long limitTotalSize) {
        this.limitTotalSize = limitTotalSize;
    }

    public void setLimitTotal(boolean limitTotal) {
        this.limitTotal = limitTotal;
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
        if (isSet(paymentStates) || isSet(refundStates)) {
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
                queryMap.put(FROM_SETTLED_DATE_FIELD, ZonedDateTime.parse(fromSettledDate));
            }
            if (isNotBlank(toSettledDate)) {
                queryMap.put(TO_SETTLED_DATE_FIELD, ZonedDateTime.parse(toSettledDate));
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

    private List<String> getTransactionStatesForStatuses(CommaDelimitedSetParameter paymentStates) {
        return paymentStates.getParameters().stream()
                .map(status -> getTransactionState(status, statusVersion))
                .flatMap(List::stream)
                .map(TransactionState::name)
                .collect(Collectors.toList());
    }

    private String createStateFilter() {
        String paymentStateFilter = null;
        String refundStateFilter = null;
        if (isSet(paymentStates)) {
            paymentStateFilter =
                    " (t.state IN (<" + PAYMENT_STATES_FIELD + ">) AND t.type =  'PAYMENT'::transaction_type)";
        }
        if (isSet(refundStates)) {
            refundStateFilter =
                    " (t.state IN (<" + REFUND_STATES_FIELD + ">) AND t.type =  'REFUND'::transaction_type)";
        }

        return "(" + List.of(
                Optional.ofNullable(paymentStateFilter), Optional.ofNullable(refundStateFilter)
        ).stream()
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" OR ")) + ")";
    }

    private String likeClause(String rawUserInputText) {
        return "%" + rawUserInputText + "%";
    }

    public void setExactReferenceMatch(boolean exactReferenceMatch) {
        this.exactReferenceMatch = exactReferenceMatch;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }
}
