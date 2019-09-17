package uk.gov.pay.ledger.transaction.search.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParams {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionSearchParams.class);

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
    private static final long MAX_DISPLAY_SIZE = 500;
    private static final long DEFAULT_PAGE_NUMBER = 1L;

    private String accountId;
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
    private Long pageNumber = 1L;
    private Long displaySize = MAX_DISPLAY_SIZE;
    private Map<String, Object> queryMap;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    @QueryParam("page")
    public void setPageNumber(Long pageNumber) {
        if (pageNumber == null) {
            this.pageNumber = DEFAULT_PAGE_NUMBER;
        } else {
            this.pageNumber = pageNumber;
        }
    }

    @QueryParam("display_size")
    public void setDisplaySize(Long displaySize) {
        if (displaySize == null || displaySize > MAX_DISPLAY_SIZE) {
            LOGGER.info("Invalid display_size [{}] for transaction search params. Setting display_size to default [{}]",
                    displaySize, MAX_DISPLAY_SIZE);
            this.displaySize = MAX_DISPLAY_SIZE;
        } else {
            this.displaySize = displaySize;
        }
    }

    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (isNotBlank(accountId)) {
            filters.add(" t.gateway_account_id = :" + GATEWAY_ACCOUNT_EXTERNAL_FIELD);
        }
        if (transactionType != null) {
            filters.add(" t.type = :" + TRANSACTION_TYPE_FIELD + "::transaction_type");
        }
        if (isNotBlank(email)) {
            filters.add(" lower(t.email) ILIKE :" + EMAIL_FIELD);
        }
        if (isNotBlank(reference)) {
            filters.add(" lower(t.reference) ILIKE :" + REFERENCE_FIELD);
        }
        if (isNotBlank(cardHolderName)) {
            filters.add(" lower(t.cardholder_name) ILIKE :" + CARDHOLDER_NAME_FIELD);
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
        if (cardBrands != null && !cardBrands.isEmpty()) {
            filters.add(" t.card_brand IN (<" + CARD_BRAND_FIELD + ">)");
        }
        if (isNotBlank(lastDigitsCardNumber)) {
            filters.add(" t.last_digits_card_number = :" + LAST_DIGITS_CARD_NUMBER_FIELD);
        }
        if (isNotBlank(firstDigitsCardNumber)) {
            filters.add(" t.first_digits_card_number = :" + FIRST_DIGITS_CARD_NUMBER_FIELD);
        }

        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (isNotBlank(accountId)) {
                queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountId);
            }

            if (isNotBlank(email)) {
                queryMap.put(EMAIL_FIELD, likeClause(email));
            }
            if (isNotBlank(reference)) {
                queryMap.put(REFERENCE_FIELD, likeClause(reference));
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
                queryMap.put(STATE_FIELD, TransactionState.getStatesForStatus(state).stream()
                        .map(TransactionState::name)
                        .collect(Collectors.toList()));
            }
            if (cardBrands != null && !cardBrands.isEmpty()) {
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
        }
        return queryMap;
    }

    public String getAccountId() {
        return accountId;
    }

    public Long getPageNumber() {
        return pageNumber;
    }

    public Long getDisplaySize() {
        return displaySize;
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

    public String buildQueryParamString(Long forPage) {
        List<String> queries = new ArrayList<>();

        if (isNotBlank(accountId)) {
            queries.add(GATEWAY_ACCOUNT_EXTERNAL_FIELD + "=" + accountId);
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
        if (refundStates != null && !refundStates.isEmpty()) {
            queries.add(REFUND_STATES_FIELD + "=" + refundStates.getRawString());
        }
        if (cardBrands != null && !cardBrands.isEmpty()) {
            queries.add(CARD_BRAND_FIELD + "=" + cardBrands.getRawString());
        }
        if (isNotBlank(state)) {
            queries.add(STATE_FIELD + "=" + state);
        }
        if (transactionType != null) {
            queries.add(TRANSACTION_TYPE_FIELD + "=" + transactionType);
        }
        queries.add("page=" + forPage);
        queries.add("display_size=" + displaySize);


        return String.join("&", queries);
    }

    public Long getOffset() {
        Long offset = 0L;

        if (pageNumber != null) {
            offset = (pageNumber - 1) * getDisplaySize();
        }

        return offset;
    }

    private List<String> getTransactionStatesForStatuses(CommaDelimitedSetParameter paymentStates) {
        return paymentStates.getParameters().stream()
                .map(TransactionState::getStatesForStatus)
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

    private boolean isSet(CommaDelimitedSetParameter commaDelimitedSetParameter) {
        return commaDelimitedSetParameter != null && !commaDelimitedSetParameter.isEmpty();
    }
}
