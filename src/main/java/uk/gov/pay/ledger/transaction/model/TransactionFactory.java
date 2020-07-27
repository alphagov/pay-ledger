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

            return new Payment.Builder()
                    .withGatewayAccountId(entity.getGatewayAccountId())
                    .withAmount(entity.getAmount())
                    .withReference(entity.getReference())
                    .withDescription(entity.getDescription())
                    .withState(entity.getState())
                    .withLanguage(safeGetAsString(transactionDetails, "language"))
                    .withExternalId(entity.getExternalId())
                    .withReturnUrl(safeGetAsString(transactionDetails, "return_url"))
                    .withEmail(entity.getEmail())
                    .withPaymentProvider(safeGetAsString(transactionDetails, "payment_provider"))
                    .withCreatedDate(entity.getCreatedDate())
                    .withCardDetails(cardDetails)
                    .withDelayedCapture(safeGetAsBoolean(transactionDetails, "delayed_capture", false))
                    .withExternalMetadata(metadata)
                    .withEventCount(entity.getEventCount())
                    .withGatewayTransactionId(safeGetAsString(transactionDetails, "gateway_transaction_id"))
                    .withCorporateCardSurcharge(safeGetAsLong(transactionDetails, "corporate_surcharge"))
                    .withFee(entity.getFee())
                    .withNetAmount(entity.getNetAmount())
                    .withRefundSummary(refundSummary)
                    .withTotalAmount(entity.getTotalAmount())
                    .withSettlementSummary(settlementSummary)
                    .withMoto(entity.isMoto())
                    .withLive(entity.isLive())
                    .withSource(entity.getSource())
                    .withWalletType(safeGetAsString(transactionDetails, "wallet"))
                    .withGatewayPayoutId(entity.getGatewayPayoutId())
                    .build();
        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]", entity.getExternalId(), e.getMessage());
        }

        return null;
    }

    private Transaction createRefund(TransactionEntity entity) {
        try {
            JsonNode transactionDetails = objectMapper.readTree(Optional.ofNullable(entity.getTransactionDetails()).orElse("{}"));

            JsonNode refundPaymentDetails = transactionDetails.get("payment_details");

            CardType cardType = CardType.fromString(safeGetAsString(refundPaymentDetails, "card_type"));
            CardDetails cardDetails = CardDetails.from(entity.getCardholderName(), null,
                    safeGetAsString(refundPaymentDetails, "card_brand_label"), entity.getLastDigitsCardNumber(),
                    entity.getFirstDigitsCardNumber(), safeGetAsString(refundPaymentDetails, "expiry_date"), cardType);

            Payment paymentDetails = new Payment.Builder()
                    .withReference(entity.getReference())
                    .withDescription(entity.getDescription())
                    .withEmail(entity.getEmail())
                    .withCardDetails(cardDetails)
                    .withWalletType(safeGetAsString(refundPaymentDetails, "wallet"))
                    .build();

            return new Refund.Builder()
                    .withGatewayAccountId(entity.getGatewayAccountId())
                    .withAmount(entity.getAmount())
                    .withGatewayTransactionId(entity.getGatewayTransactionId())
                    .withState(entity.getState())
                    .withExternalId(entity.getExternalId())
                    .withCreatedDate(entity.getCreatedDate())
                    .withEventCount(entity.getEventCount())
                    .withRefundedBy(safeGetAsString(transactionDetails, "refunded_by"))
                    .withRefundedByUserEmail(safeGetAsString(transactionDetails, "user_email"))
                    .withParentExternalId(entity.getParentExternalId())
                    .withGatewayPayoutId(entity.getGatewayPayoutId())
                    .withPaymentDetails(paymentDetails)
                    .build();

        } catch (IOException e) {
            LOGGER.error("Error during the parsing transaction entity data [{}] [errorMessage={}]", entity.getExternalId(), e.getMessage());
        }

        return null;
    }
}
