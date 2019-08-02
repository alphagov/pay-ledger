package uk.gov.pay.ledger.transaction.search.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.model.TransactionType;

import javax.ws.rs.QueryParam;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParams {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionSearchParams.class);

    private static final String GATEWAY_ACCOUNT_EXTERNAL_FIELD = "account_id";
    private static final String OFFSET_FIELD = "offset";
    private static final String PAGE_SIZE_FIELD = "limit";
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

    @QueryParam("account_id")
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

    public String generateQuery() {
        StringBuilder sb = new StringBuilder();

        if (isNotBlank(email)) {
            sb.append(" AND t.email ILIKE :" + EMAIL_FIELD);
        }
        if (isNotBlank(reference)) {
            sb.append(" AND t.reference ILIKE :" + REFERENCE_FIELD);
        }
        if (isNotBlank(cardHolderName)) {
            sb.append(" AND t.cardholder_name ILIKE :" + CARDHOLDER_NAME_FIELD);
        }
        if (fromDate != null) {
            sb.append(" AND t.created_date > :" + FROM_DATE_FIELD);
        }
        if (toDate != null) {
            sb.append(" AND t.created_date < :" + TO_DATE_FIELD);
        }
        if (paymentStates != null && !paymentStates.isEmpty()) {
            //TODO implement
        }
        if (isNotBlank(state)) {
            sb.append(" AND t.state = :" + STATE_FIELD);
        }
        if (refundStates != null) {
            //TODO implement
        }
        if (cardBrands != null && !cardBrands.isEmpty()) {
            sb.append(" AND t.card_brand IN(<" + CARD_BRAND_FIELD + ">)");
        }
        if (isNotBlank(lastDigitsCardNumber)) {
            sb.append(" AND t.last_digits_card_number = :" + LAST_DIGITS_CARD_NUMBER_FIELD);
        }
        if (isNotBlank(firstDigitsCardNumber)) {
            sb.append(" AND t.first_digits_card_number = :" + FIRST_DIGITS_CARD_NUMBER_FIELD);
        }
        if (transactionType != null) {
            sb.append(" AND t.type = :" + TRANSACTION_TYPE_FIELD + "::transaction_type");
        }

        return sb.toString();
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();
            queryMap.put(GATEWAY_ACCOUNT_EXTERNAL_FIELD, accountId);
            queryMap.put(OFFSET_FIELD, getOffset());
            queryMap.put(PAGE_SIZE_FIELD, displaySize);

            if (isNotBlank(email)) {
                queryMap.put(EMAIL_FIELD, likeClause(email));
            }
            if (isNotBlank(reference)) {
                queryMap.put(REFERENCE_FIELD, likeClause(reference));
            }
            if (cardHolderName != null) {
                queryMap.put(CARDHOLDER_NAME_FIELD, likeClause(cardHolderName));
            }
            if (fromDate != null) {
                queryMap.put(FROM_DATE_FIELD, ZonedDateTime.parse(fromDate));
            }
            if (toDate != null) {
                queryMap.put(TO_DATE_FIELD, ZonedDateTime.parse(toDate));
            }
            if (isNotBlank(state)) {
                queryMap.put(STATE_FIELD, state);
            }
            if (cardBrands != null && !cardBrands.isEmpty()) {
                queryMap.put(CARD_BRAND_FIELD, cardBrands.getParameters());
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
        String query = GATEWAY_ACCOUNT_EXTERNAL_FIELD + "=" + accountId;

        if (fromDate != null) {
            query += "&" + FROM_DATE_FIELD + "=" + fromDate;
        }
        if (toDate != null) {
            query += "&" + TO_DATE_FIELD + "=" + toDate;
        }

        if (isNotBlank(email)) {
            query += "&" + EMAIL_FIELD + "=" + email;
        }
        if (isNotBlank(reference)) {
            query += "&" + REFERENCE_FIELD + "=" + reference;
        }
        if (isNotBlank(cardHolderName)) {
            query += "&" + CARDHOLDER_NAME_FIELD + "=" + cardHolderName;
        }
        if (isNotBlank(firstDigitsCardNumber)) {
            query += "&" + FIRST_DIGITS_CARD_NUMBER_FIELD + "=" + firstDigitsCardNumber;
        }
        if (isNotBlank(lastDigitsCardNumber)) {
            query += "&" + LAST_DIGITS_CARD_NUMBER_FIELD + "=" + lastDigitsCardNumber;
        }
        if (paymentStates != null && !paymentStates.isEmpty()) {
            query += "&" + PAYMENT_STATES_FIELD + "=" + paymentStates.getRawString();
        }
        if (refundStates != null && !refundStates.isEmpty()) {
            query += "&" + REFUND_STATES_FIELD + "=" + refundStates.getRawString();
        }
        if (cardBrands != null && !cardBrands.isEmpty()) {
            query += "&" + CARD_BRAND_FIELD + "=" + cardBrands.getRawString();
        }
        if (isNotBlank(state)) {
            query += "&" + STATE_FIELD + "=" + state;
        }
        if(transactionType != null) {
            query += "&" + TRANSACTION_TYPE_FIELD + "=" + transactionType;
        }
        //todo: potentially replace the whole shebang with string builder (efficient doh!)

        query += addPaginationParams(forPage);
        return query;
    }

    private Long getOffset() {
        Long offset = 0l;

        if (pageNumber != null) {
            offset = (pageNumber - 1) * getDisplaySize();
        }

        return offset;
    }

    private String likeClause(String rawUserInputText) {
        return "%" + rawUserInputText + "%";
    }

    private String addPaginationParams(Long forPage) {
        String queryParams = format("&page=%s", forPage);
        queryParams += format("&display_size=%s", displaySize.intValue());
        return queryParams;
    }


}
