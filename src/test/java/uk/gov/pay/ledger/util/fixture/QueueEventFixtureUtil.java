package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class QueueEventFixtureUtil {

    public static String insert(AmazonSQS sqsClient, String eventType, ZonedDateTime eventDate, String serviceId, Boolean live, String resourceExternalId,
                                           String parentResourceExternalId, ResourceType resourceType, String eventData) {
        String messageBody = String.format("{" +
                        "\"timestamp\": \"%s\"," +
                        "\"resource_external_id\": \"%s\"," +
                        "\"service_id\": \"%s\"," +
                        "\"live\": \"%s\"," +
                        (parentResourceExternalId == null || parentResourceExternalId.isEmpty() ? "%s" : "\"parent_resource_external_id\": \"%s\",") +
                        "\"event_type\":\"%s\"," +
                        "\"resource_type\": \"%s\"," +
                        "\"event_details\": %s" +
                        "}",
                eventDate.toString(),
                resourceExternalId,
                serviceId,
                live,
                parentResourceExternalId == null ? "" : parentResourceExternalId,
                eventType,
                resourceType.toString().toLowerCase(),
                eventData
        );

        SendMessageResult result = sqsClient.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), messageBody);
        return result.getMessageId();
    }

    public static PactDslJsonBody getAsPact(String serviceId, Boolean live, String eventType, ZonedDateTime eventDate, String resourceExternalId,
                                     String parentResourceExternalId, ResourceType resourceType, String eventData) {
        PactDslJsonBody eventDetails = new PactDslJsonBody();

        DateTimeFormatter formatToMicroseconds = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSX");

        eventDetails.stringType("event_type", eventType);
        eventDetails.stringType("timestamp", eventDate.format(formatToMicroseconds));
        eventDetails.stringType("resource_external_id", resourceExternalId);
        eventDetails.booleanType("live", live);
        eventDetails.stringType("resource_type", resourceType.toString().toLowerCase());
        if (parentResourceExternalId != null && !parentResourceExternalId.isEmpty()) {
            eventDetails.stringType("parent_resource_external_id", parentResourceExternalId);
        }
        if (serviceId != null) {
            eventDetails.stringType("service_id", serviceId);
        }
        if (live != null ) {
            eventDetails.booleanType("live", live);
        }

        PactDslJsonBody eventDetailsPact = getNestedPact(JsonParser.parseString(eventData).getAsJsonObject());

        eventDetails.object("event_details", eventDetailsPact);

        return eventDetails;
    }

    private static PactDslJsonBody getNestedPact(JsonObject eventData) {
        PactDslJsonBody dslJsonBody = new PactDslJsonBody();
        eventData.entrySet()
                .forEach(e -> {
                    try {
                        if(e.getValue().isJsonPrimitive()) {
                            JsonPrimitive value = (JsonPrimitive) e.getValue();

                            if (value.isNumber()) {
                                dslJsonBody.integerType(e.getKey(), value.getAsInt());
                            } else if (value.isBoolean()) {
                                dslJsonBody.booleanType(e.getKey(), value.getAsBoolean());
                            } else {
                                dslJsonBody.stringType(e.getKey(), value.getAsString());
                            }
                        } else {
                            dslJsonBody.object(e.getKey(), getNestedPact(e.getValue().getAsJsonObject()));
                        }
                    } catch (Exception ex) {
                        dslJsonBody.stringType(e.getKey(), e.getValue().getAsString());
                    }
                });

        return dslJsonBody;
    }
}
