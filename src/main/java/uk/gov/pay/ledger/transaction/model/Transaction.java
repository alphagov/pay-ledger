package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class Transaction {

    @JsonIgnore
    protected Long id;
    protected String gatewayAccountId;
    protected Long amount;
    protected String externalId;

    public Transaction() {
    }

    public Transaction(Long id, String gatewayAccountId, Long amount, String externalId) {
        this.id = id;
        this.gatewayAccountId = gatewayAccountId;
        this.amount = amount;
        this.externalId = externalId;
    }

    protected static Long safeGetAsLong(JsonObject object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonElement::getAsLong)
                .orElse(null);
    }

    protected static String safeGetAsString(JsonObject object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonElement::getAsString)
                .orElse(null);
    }

    protected static JsonObject safeGetAsObject(JsonObject object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonElement::getAsJsonObject)
                .orElse(null);
    }

    protected static JsonArray safeGetAsArray(JsonObject object, String propertyName) {
        return safeGetJsonElement(object, propertyName)
                .map(JsonElement::getAsJsonArray)
                .orElse(null);
    }

    protected static Optional<JsonElement> safeGetJsonElement(JsonObject object, String propertyName) {
        return Optional.ofNullable(object.get(propertyName))
                .filter(p -> !p.isJsonNull());
    }

    public Long getId() {
        return id;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getExternalId() {
        return externalId;
    }
}
