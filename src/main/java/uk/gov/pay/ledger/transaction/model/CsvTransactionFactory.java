package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.ExternalTransactionState;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static java.math.BigDecimal.valueOf;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.ledger.util.JsonParser.safeGetAsLong;
import static uk.gov.pay.ledger.util.JsonParser.safeGetAsString;
import static uk.gov.pay.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;

public class CsvTransactionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvTransactionFactory.class);
    private static final String FIELD_AMOUNT = "Amount";
    private static final String FIELD_REFERENCE = "Reference";
    private static final String FIELD_DESC = "Description";
    private static final String FIELD_DATE_CREATED = "Date Created";
    private static final String FIELD_TIME_CREATED = "Time Created";
    private static final String FIELD_EMAIL = "Email";
    private static final String FIELD_CARD_BRAND = "Card Brand";
    private static final String FIELD_CARDHOLDER_NAME = "Cardholder Name";
    private static final String FIELD_CARD_EXPIRY_DATE = "Card Expiry Date";
    private static final String FIELD_CARD_NUMBER = "Card Number";
    private static final String FIELD_STATE = "State";
    private static final String FIELD_FINISHED = "Finished";
    private static final String FIELD_ERROR_CODE = "Error Code";
    private static final String FIELD_ERROR_MESSAGE = "Error Message";
    private static final String FIELD_PROVIDER_ID = "Provider ID";
    private static final String FIELD_GOVUK_PAYMENT_ID = "GOV.UK Payment ID";
    private static final String FIELD_ISSUED_BY = "Issued By";
    private static final String FIELD_CORPORATE_CARD_SURCHARGE = "Corporate Card Surcharge";
    private static final String FIELD_TOTAL_AMOUNT = "Total Amount";
    private static final String FIELD_WALLET_TYPE = "Wallet Type";
    private static final String FIELD_CARD_TYPE = "Card Type";
    private static final String FIELD_FEE = "Fee";
    private static final String FIELD_NET = "Net";
    private ObjectMapper objectMapper;

    @Inject
    public CsvTransactionFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> toMap(TransactionEntity transactionEntity) {
        Map<String, Object> result = new HashMap<>();

        try {
            String dateCreated = parseDateForPattern(transactionEntity.getCreatedDate(),
                    "dd MMM yyyy");
            String timeCreated = parseDateForPattern(transactionEntity.getCreatedDate(),
                    "HH:mm:ss");

            JsonNode transactionDetails = objectMapper.readTree(
                    Optional.ofNullable(transactionEntity.getTransactionDetails()).orElse("{}"));

            result.put(FIELD_REFERENCE, transactionEntity.getReference());
            result.put(FIELD_DESC, transactionEntity.getDescription());
            result.put(FIELD_EMAIL, transactionEntity.getEmail());
            result.put(FIELD_AMOUNT, penceToCurrency(transactionEntity.getAmount()));
            result.put(FIELD_CARD_BRAND, safeGetAsString(transactionDetails, "card_brand_label"));
            result.put(FIELD_CARDHOLDER_NAME, transactionEntity.getCardholderName());
            result.put(FIELD_CARD_EXPIRY_DATE, safeGetAsString(transactionDetails, "expiry_date"));
            result.put(FIELD_CARD_NUMBER, transactionEntity.getLastDigitsCardNumber());
            result.put(FIELD_PROVIDER_ID, safeGetAsString(transactionDetails, "gateway_transaction_id"));
            result.put(FIELD_GOVUK_PAYMENT_ID, transactionEntity.getExternalId());
            result.put(FIELD_ISSUED_BY, safeGetAsString(transactionDetails, "user_email"));
            result.put(FIELD_DATE_CREATED, dateCreated);
            result.put(FIELD_TIME_CREATED, timeCreated);
            result.put(FIELD_CORPORATE_CARD_SURCHARGE,
                    penceToCurrency(safeGetAsLong(transactionDetails, "corporate_surcharge")));
            result.put(FIELD_TOTAL_AMOUNT, penceToCurrency(transactionEntity.getTotalAmount()));
            result.put(FIELD_WALLET_TYPE, safeGetAsString(transactionDetails, "wallet_type"));
            result.put(FIELD_CARD_TYPE, safeGetAsString(transactionDetails, "card_type"));

            if (transactionEntity.getState() != null) {
                ExternalTransactionState state = ExternalTransactionState.from(transactionEntity.getState(), 2);
                result.put(FIELD_STATE, state.getStatus());
                result.put(FIELD_FINISHED, state.isFinished());
                result.put(FIELD_ERROR_CODE, state.getCode());
                result.put(FIELD_ERROR_MESSAGE, state.getMessage());
            }

            result.put(FIELD_FEE, penceToCurrency(transactionEntity.getFee()));
            result.put(FIELD_NET, penceToCurrency(transactionEntity.getNetAmount()));

            Optional<Map<String, Object>> externalMetadata = getExternalMetadata(transactionEntity.getTransactionDetails());

            externalMetadata.ifPresent(metadata ->
                    metadata.forEach((key, value) ->
                            result.put(String.format("%s (metadata)", key), value)));
        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]",
                    transactionEntity.getExternalId(), e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getCsvHeaders(List<TransactionEntity> transactions) {
        LinkedHashMap<String, Object> headers = new LinkedHashMap<>();

        headers.put(FIELD_REFERENCE, FIELD_REFERENCE);
        headers.put(FIELD_DESC, FIELD_DESC);
        headers.put(FIELD_EMAIL, FIELD_EMAIL);
        headers.put(FIELD_AMOUNT, FIELD_AMOUNT);
        headers.put(FIELD_CARD_BRAND, FIELD_CARD_BRAND);
        headers.put(FIELD_CARDHOLDER_NAME, FIELD_CARDHOLDER_NAME);
        headers.put(FIELD_CARD_EXPIRY_DATE, FIELD_CARD_EXPIRY_DATE);
        headers.put(FIELD_CARD_NUMBER, FIELD_CARD_NUMBER);
        headers.put(FIELD_STATE, FIELD_STATE);
        headers.put(FIELD_FINISHED, FIELD_FINISHED);
        headers.put(FIELD_ERROR_CODE, FIELD_ERROR_CODE);
        headers.put(FIELD_ERROR_MESSAGE, FIELD_ERROR_MESSAGE);
        headers.put(FIELD_PROVIDER_ID, FIELD_PROVIDER_ID);
        headers.put(FIELD_GOVUK_PAYMENT_ID, FIELD_GOVUK_PAYMENT_ID);
        headers.put(FIELD_ISSUED_BY, FIELD_ISSUED_BY);
        headers.put(FIELD_DATE_CREATED, FIELD_DATE_CREATED);
        headers.put(FIELD_TIME_CREATED, FIELD_TIME_CREATED);
        headers.put(FIELD_CORPORATE_CARD_SURCHARGE, FIELD_CORPORATE_CARD_SURCHARGE);
        headers.put(FIELD_TOTAL_AMOUNT, FIELD_TOTAL_AMOUNT);
        headers.put(FIELD_WALLET_TYPE, FIELD_WALLET_TYPE);

        if (isStripeTransaction(transactions.get(0))) {
            headers.put(FIELD_FEE, FIELD_FEE);
            headers.put(FIELD_NET, FIELD_NET);
        }

        Set<String> metadataHeaders = deriveMetadataHeaders(transactions);
        metadataHeaders.forEach(key -> headers.put(key, key));

        headers.put(FIELD_CARD_TYPE, FIELD_CARD_TYPE);
        return headers;
    }

    private boolean isStripeTransaction(TransactionEntity aTransaction) {
        try {
            JsonNode transactionDetails = objectMapper.readTree(Optional.ofNullable(
                    aTransaction.getTransactionDetails()).orElse("{}"));
            String paymentProvider = safeGetAsString(transactionDetails, "payment_provider");

            return "stripe".equals(paymentProvider);
        } catch (IOException e) {
            LOGGER.error("Error parsing transaction for Stripe specific CSV headers [{}] [errorMessage={}]",
                    kv(PAYMENT_EXTERNAL_ID, aTransaction.getExternalId()),
                    e.getMessage());
            return false;
        }
    }

    private String penceToCurrency(Long amount) {
        if (amount != null) {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            return decimalFormat.format(valueOf(amount).divide(valueOf(100L)));
        }
        return null;
    }

    private String parseDateForPattern(ZonedDateTime dateTime, String pattern) {
        return Optional.ofNullable(dateTime)
                .map(date -> date.format(DateTimeFormatter.ofPattern(pattern)))
                .orElse(null);
    }

    private Optional<Map<String, Object>> getExternalMetadata(String transactionDetails)
            throws IOException {

        JsonNode transactionDetailsJsonNode = objectMapper.readTree(
                Optional.ofNullable(transactionDetails).orElse("{}"));

        Map<String, Object> metadata = null;
        if (transactionDetailsJsonNode.has("external_metadata")) {
            metadata = objectMapper.readValue(
                    objectMapper.treeAsTokens(transactionDetailsJsonNode.get("external_metadata")),
                    new TypeReference<Map<String, Object>>() {
                    });
        }
        return Optional.ofNullable(metadata);
    }

    private Set<String> deriveMetadataHeaders(List<TransactionEntity> transactions) {
        Set<String> metadataHeaders = new TreeSet<>();
        transactions.forEach(transactionEntity -> {
            try {
                Optional<Map<String, Object>> externalMetadata = getExternalMetadata(
                        transactionEntity.getTransactionDetails());

                externalMetadata.ifPresent(metadata ->
                        metadata.forEach((key, value) ->
                                metadataHeaders.add(String.format("%s (metadata)", key))));
            } catch (IOException e) {
                LOGGER.error("Error parsing transaction for metadata headers [{}] [errorMessage={}]",
                        kv(PAYMENT_EXTERNAL_ID, transactionEntity.getExternalId()),
                        e.getMessage());
            }
        });
        return metadataHeaders;
    }
}
