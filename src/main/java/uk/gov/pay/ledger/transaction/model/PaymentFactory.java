package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.search.model.Link;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.pay.ledger.transaction.model.Transaction.safeGetAsArray;
import static uk.gov.pay.ledger.transaction.model.Transaction.safeGetAsLong;
import static uk.gov.pay.ledger.transaction.model.Transaction.safeGetAsObject;
import static uk.gov.pay.ledger.transaction.model.Transaction.safeGetAsString;

public class PaymentFactory {

    private ObjectMapper objectMapper;

    @Inject
    public PaymentFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Payment fromTransactionEntity(TransactionEntity entity) {
        JsonObject transactionDetail = new JsonParser().parse(entity.getTransactionDetails()).getAsJsonObject();
        Address billingAddress = new Address(
                safeGetAsString(transactionDetail, "address_line1"),
                safeGetAsString(transactionDetail, "address_line2"),
                safeGetAsString(transactionDetail, "address_postcode"),
                safeGetAsString(transactionDetail, "address_city"),
                safeGetAsString(transactionDetail, "address_county"),
                safeGetAsString(transactionDetail, "address_country")
        );

        CardDetails cardDetails = new CardDetails(entity.getCardholderName(), billingAddress, entity.getCardBrand(),
                entity.getLastDigitsCardNumber(), entity.getFirstDigitsCardNumber(),
                safeGetAsString(transactionDetail, "card_expiry_date"));

        Map<String, Object> metadata = null;
        List<Link> links = null;
        RefundSummary refundSummary = null;
        try {
            metadata = entity.getExternalMetadata() != null ? objectMapper.readValue(entity.getExternalMetadata(), Map.class) : null;
            Type listType = new TypeToken<ArrayList<Link>>(){}.getType();
            links = new Gson().fromJson(safeGetAsArray(transactionDetail, "links"), listType);
            var refundSummaryObject = safeGetAsObject(transactionDetail, "refund_summary");
            if(refundSummaryObject != null) {
                refundSummary = RefundSummary.ofValue(
                        safeGetAsString(refundSummaryObject, "status"),
                        safeGetAsLong(refundSummaryObject, "amount_available"),
                        safeGetAsLong(refundSummaryObject, "amount_submitted"));
            }
        } catch (Exception e) {
            e.printStackTrace(); //todo
        }

        return new Payment(entity.getGatewayAccountId(), entity.getAmount(), entity.getReference(), entity.getDescription(),
                TransactionState.from(entity.getState()), safeGetAsString(transactionDetail, "language"),
                entity.getExternalId(), safeGetAsString(transactionDetail, "return_url"), entity.getEmail(),
                safeGetAsString(transactionDetail, "payment_provider"), entity.getCreatedDate(),
                cardDetails, Boolean.valueOf(safeGetAsString(transactionDetail, "delayed_capture")),
                metadata, entity.getEventCount(), safeGetAsString(transactionDetail, "gateway_transaction_id"),
                safeGetAsLong(transactionDetail, "corporate_surcharge"), safeGetAsLong(transactionDetail, "fee"),
                safeGetAsLong(transactionDetail, "net_amount"), refundSummary, links);
    }
}
