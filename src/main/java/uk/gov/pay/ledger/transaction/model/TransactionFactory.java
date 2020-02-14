package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.search.model.SettlementSummary;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.ledger.util.JsonParser.safeGetAsBoolean;
import static uk.gov.pay.ledger.util.JsonParser.safeGetAsDate;
import static uk.gov.pay.ledger.util.JsonParser.safeGetAsLong;
import static uk.gov.pay.ledger.util.JsonParser.safeGetAsString;

public class TransactionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionFactory.class);
    private ObjectMapper objectMapper;

    @Inject
    public TransactionFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Transaction createTransactionEntity(TransactionEntity entity) {
        if ("REFUND".equals(entity.getTransactionType())) {
            return createRefund(entity);
        }

        return createPayment(entity);
    }

    private Transaction createPayment(TransactionEntity entity) {
        try {
            JsonNode transactionDetails = objectMapper.readTree(Optional.ofNullable(entity.getTransactionDetails()).orElse("{}"));
            Address billingAddress = Address.from(
                    safeGetAsString(transactionDetails, "address_line1"),
                    safeGetAsString(transactionDetails, "address_line2"),
                    safeGetAsString(transactionDetails, "address_postcode"),
                    safeGetAsString(transactionDetails, "address_city"),
                    safeGetAsString(transactionDetails, "address_county"),
                    safeGetAsString(transactionDetails, "address_country")
            );
            String cardBrand = safeGetAsString(transactionDetails, "card_brand_label");

            CardType cardType = CardType.fromString(safeGetAsString(transactionDetails, "card_type"));
            CardDetails cardDetails = CardDetails.from(entity.getCardholderName(), billingAddress, cardBrand,
                    entity.getLastDigitsCardNumber(), entity.getFirstDigitsCardNumber(),
                    safeGetAsString(transactionDetails, "expiry_date"), cardType);

            Map<String, Object> metadata = null;
            if (transactionDetails.has("external_metadata")) {
                metadata = objectMapper.readValue(
                        objectMapper.treeAsTokens(transactionDetails.get("external_metadata")),
                        new TypeReference<>() {
                        });
            }

            RefundSummary refundSummary = RefundSummary.from(entity);
            SettlementSummary settlementSummary = new SettlementSummary(
                    safeGetAsDate(transactionDetails, "capture_submitted_date"),
                    safeGetAsDate(transactionDetails, "captured_date")
            );

            return new Payment(
                    entity.getGatewayAccountId(),
                    entity.getAmount(),
                    entity.getReference(),
                    entity.getDescription(),
                    entity.getState(),
                    safeGetAsString(transactionDetails, "language"),
                    entity.getExternalId(),
                    safeGetAsString(transactionDetails, "return_url"),
                    entity.getEmail(),
                    safeGetAsString(transactionDetails, "payment_provider"),
                    entity.getCreatedDate(),
                    cardDetails,
                    safeGetAsBoolean(transactionDetails, "delayed_capture", false),
                    metadata,
                    entity.getEventCount(),
                    safeGetAsString(transactionDetails, "gateway_transaction_id"),
                    safeGetAsLong(transactionDetails, "corporate_surcharge"),
                    entity.getFee(),
                    entity.getNetAmount(),
                    refundSummary,
                    entity.getTotalAmount(),
                    settlementSummary,
                    entity.isMoto(),
                    entity.isLive(),
                    entity.getSource(),
                    safeGetAsString(transactionDetails, "wallet")
            );
        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]", entity.getExternalId(), e.getMessage());
        }

        return null;
    }

    private Transaction createRefund(TransactionEntity entity) {
        try {
            JsonNode transactionDetails = objectMapper.readTree(Optional.ofNullable(entity.getTransactionDetails()).orElse("{}"));

            Optional<Transaction> parentTransaction = Optional.ofNullable(entity.getParentTransactionEntity())
                    .map(this::createTransactionEntity);

            return new Refund.Builder()
                    .withGatewayAccountId(entity.getGatewayAccountId())
                    .withAmount(entity.getAmount())
                    .withReference(entity.getReference())
                    .withDescription(entity.getDescription())
                    .withState(entity.getState())
                    .withExternalId(entity.getExternalId())
                    .withCreatedDate(entity.getCreatedDate())
                    .withEventCount(entity.getEventCount())
                    .withRefundedBy(safeGetAsString(transactionDetails, "refunded_by"))
                    .withRefundedByUserEmail(safeGetAsString(transactionDetails, "user_email"))
                    .withParentExternalId(entity.getParentExternalId())
                    .withParentTransaction(parentTransaction)
                    .build();

        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]", entity.getExternalId(), e.getMessage());
        }

        return null;
    }
}
