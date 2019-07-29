package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.time.ZonedDateTime;

public class QueueEventFixtureUtil {

    public static String insert(AmazonSQS sqsClient, String eventType, ZonedDateTime eventDate, String resourceExternalId,
                                           String parentResourceExternalId, ResourceType resourceType, String eventData) {
        String messageBody = String.format("{" +
                        "\"timestamp\": \"%s\"," +
                        "\"resource_external_id\": \"%s\"," +
                        (parentResourceExternalId == null || parentResourceExternalId.isEmpty() ? "%s" : "\"parent_resource_external_id\": \"%s\",") +
                        "\"event_type\":\"%s\"," +
                        "\"resource_type\": \"%s\"," +
                        "\"event_details\": %s" +
                        "}",
                eventDate.toString(),
                resourceExternalId,
                parentResourceExternalId == null ? "" : parentResourceExternalId,
                eventType,
                resourceType.toString().toLowerCase(),
                eventData
        );

        SendMessageResult result = sqsClient.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), messageBody);
        return result.getMessageId();
    }

    public static PactDslJsonBody getAsPact(String eventType, ZonedDateTime eventDate, String resourceExternalId,
                                     String parentResourceExternalId, ResourceType resourceType, String eventData) {
        PactDslJsonBody eventDetails = new PactDslJsonBody();

        eventDetails.stringType("event_type", eventType);
        eventDetails.stringType("timestamp", eventDate.toString());
        eventDetails.stringType("resource_external_id", resourceExternalId);
        eventDetails.stringType("resource_type", resourceType.toString().toLowerCase());
        if (parentResourceExternalId != null && !parentResourceExternalId.isEmpty()) {
            eventDetails.stringType("parent_resource_external_id", parentResourceExternalId);
        }

        PactDslJsonBody paymentCreatedEventDetails = new PactDslJsonBody();
        new JsonParser().parse(eventData).getAsJsonObject().entrySet()
                .forEach(e -> {
                    try {
                        JsonPrimitive value = ((JsonPrimitive) e.getValue());
                        if (value.isNumber()) {
                            paymentCreatedEventDetails.integerType(e.getKey(), value.getAsInt());
                        } else if (value.isBoolean()) {
                            paymentCreatedEventDetails.booleanType(e.getKey(), value.getAsBoolean());
                        } else {
                            paymentCreatedEventDetails.stringType(e.getKey(), value.getAsString());
                        }
                    } catch (Exception ex) {
                        paymentCreatedEventDetails.stringType(e.getKey(), e.getValue().getAsString());
                    }
                });

        eventDetails.object("event_details", paymentCreatedEventDetails);

        return eventDetails;
    }
}
