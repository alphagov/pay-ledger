package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class QueueEventFixture implements QueueFixture<QueueEventFixture, Event> {
    private String sqsMessageId;
    private ResourceType resourceType = ResourceType.PAYMENT;
    private String resourceExternalId = RandomStringUtils.randomAlphanumeric(20);
    private String parentResourceExternalId = StringUtils.EMPTY;
    private ZonedDateTime eventDate = ZonedDateTime.now(ZoneOffset.UTC);
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(5);

    private QueueEventFixture() {
    }

    public static QueueEventFixture aQueueEventFixture() {
        return new QueueEventFixture();
    }

    public QueueEventFixture withResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public QueueEventFixture withResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }

    public QueueEventFixture withParentResourceExternalId(String parentResourceExternalId) {
        this.parentResourceExternalId = parentResourceExternalId;
        return this;
    }

    public QueueEventFixture withEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public QueueEventFixture withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public QueueEventFixture withEventData(String eventData) {
        this.eventData = eventData;
        return this;
    }

    public QueueEventFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public QueueEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "PAYMENT_CREATED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("amount", 1000)
                                .put("description", "a description")
                                .put("reference", "aref")
                                .put("return_url", "https://example.org")
                                .put("gateway_account_id", gatewayAccountId)
                                .put("payment_provider", "sandbox")
                                .put("corporate_surcharge", 69)
                                .put("total_amount", 1069)
                                .build());
                break;
            case "PAYMENT_DETAILS_ENTERED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("email", "j.doe@example.org")
                                .put("language", "en")
                                .put("last_digits_card_number", "4242")
                                .put("first_digits_card_number", "424242")
                                .put("cardholder_name", "J citizen")
                                .put("expiry_date", "11/21")
                                .put("address_line1", "12 Rouge Avenue")
                                .put("address_postcode", "N1 3QU")
                                .put("address_city", "London")
                                .put("address_country", "GB")
                                .put("card_brand", "visa")
                                .put("delayed_capture", false)
                                .put("gateway_transaction_id", gatewayAccountId)
                                .build());
                break;
            default:
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.of("event_data", "event_data"));
        }
        return this;
    }

    @Override
    public Event toEntity() {
        return new Event(0L, sqsMessageId, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType, eventData);
    }

    public String getSqsMessageId() {
        return sqsMessageId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public ZonedDateTime getEventDate() {
        return eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    @Override
    public QueueEventFixture insert(AmazonSQS sqsClient) {
        String messageBody = String.format("{" +
                        "\"timestamp\": \"%s\"," +
                        "\"resource_external_id\": \"%s\"," +
                        "\"parent_resource_external_id\": \"%s\"," +
                        "\"event_type\":\"%s\"," +
                        "\"resource_type\": \"%s\"," +
                        "\"event_details\": %s" +
                        "}",
                eventDate.toString(),
                resourceExternalId,
                parentResourceExternalId,
                eventType,
                resourceType.toString().toLowerCase(),
                eventData
        );

        SendMessageResult result = sqsClient.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), messageBody);
        this.sqsMessageId = result.getMessageId();
        return this;
    }

    public PactDslJsonBody getAsPact() {
        PactDslJsonBody eventDetails = new PactDslJsonBody();

        eventDetails.stringType("event_type", eventType);
        eventDetails.stringType("timestamp", eventDate.toString());
        eventDetails.stringType("resource_external_id", resourceExternalId);
        eventDetails.stringType("resource_type", resourceType.toString().toLowerCase());

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
