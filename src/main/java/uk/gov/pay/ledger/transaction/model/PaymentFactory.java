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
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class PaymentFactory {

    private static Logger LOGGER = LoggerFactory.getLogger(PaymentFactory.class);
    private ObjectMapper objectMapper;

    @Inject
    public PaymentFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Payment createTransactionEntity(TransactionEntity entity) {
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

            CardDetails cardDetails = CardDetails.from(entity.getCardholderName(), billingAddress, entity.getCardBrand(),
                    entity.getLastDigitsCardNumber(), entity.getFirstDigitsCardNumber(),
                    safeGetAsString(transactionDetails, "card_expiry_date"));

            Map<String, Object> metadata = null;
            if(entity.getExternalMetadata() != null) {
                metadata = objectMapper.readValue(entity.getExternalMetadata(), new TypeReference<Map<String, Object>>() {});
            }

            RefundSummary refundSummary = RefundSummary.from(entity);
            SettlementSummary settlementSummary = new SettlementSummary(entity.getSettlementSubmittedTime(), entity.getSettledTime());

            return new Payment(entity.getGatewayAccountId(), entity.getAmount(), entity.getReference(), entity.getDescription(),
                    TransactionState.from(entity.getState()), safeGetAsString(transactionDetails, "language"),
                    entity.getExternalId(), safeGetAsString(transactionDetails, "return_url"), entity.getEmail(),
                    safeGetAsString(transactionDetails, "payment_provider"), entity.getCreatedDate(),
                    cardDetails, Boolean.valueOf(safeGetAsString(transactionDetails, "delayed_capture")),
                    metadata, entity.getEventCount(), safeGetAsString(transactionDetails, "gateway_transaction_id"),
                    safeGetAsLong(transactionDetails, "corporate_surcharge"), entity.getFee(),
                    entity.getNetAmount(), refundSummary, entity.getTotalAmount(), settlementSummary);
        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]", entity.getExternalId(), e.getMessage());
        }

        return null;
    }

    private static Long safeGetAsLong(JsonNode object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonNode::asLong)
                .orElse(null);
    }

    private static String safeGetAsString(JsonNode object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonNode::asText)
                .orElse(null);
    }

    private static Optional<JsonNode> safeGetJsonElement(JsonNode object, String propertyName) {
        return Optional.ofNullable(object.get(propertyName))
                .filter(p -> p != null);
    }
}
