package uk.gov.pay.ledger.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.ZonedDateTime;
import java.util.Optional;

public class JsonParser {

    private JsonParser() {
    }

    public static Long safeGetAsLong(JsonNode object, String fieldName) {
        return safeGetJsonElement(object, fieldName)
                .map(JsonNode::longValue)
                .orElse(null);
    }

    public static Boolean safeGetAsBoolean(JsonNode object, String fieldName, Boolean defaultValue) {
        return safeGetJsonElement(object, fieldName)
                .map(JsonNode::booleanValue)
                .orElse(defaultValue);
    }

    public static String safeGetAsString(JsonNode object, String fieldName) {
        return safeGetJsonElement(object, fieldName)
                .map(JsonNode::textValue)
                .orElse(null);
    }

    public static ZonedDateTime safeGetAsDate(JsonNode object, String fieldName) {
        return safeGetJsonElement(object, fieldName)
                .map(JsonNode::textValue)
                .map(ZonedDateTime::parse)
                .orElse(null);
    }

    private static Optional<JsonNode> safeGetJsonElement(JsonNode object, String fieldName) {
        return Optional.ofNullable(object.get(fieldName));
    }
}
