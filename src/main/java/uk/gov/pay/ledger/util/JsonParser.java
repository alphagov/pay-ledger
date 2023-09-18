package uk.gov.pay.ledger.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public class JsonParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        if (object == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(object.get(fieldName));
    }

    public static Map<String, Object> jsonStringToMap(String jsonString) {
        try {
            return (Map<String, Object>) objectMapper.readValue(jsonString, Map.class);
        } catch (IOException | ClassCastException e) {
            throw new RuntimeException("Error converting event Json to Map");
        }
    }
}
