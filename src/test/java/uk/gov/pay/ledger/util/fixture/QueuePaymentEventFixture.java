package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class QueuePaymentEventFixture implements QueueFixture<QueuePaymentEventFixture, Event> {
    private String sqsMessageId;
    private ResourceType resourceType = ResourceType.PAYMENT;
    private String resourceExternalId = RandomStringUtils.randomAlphanumeric(20);
    private String parentResourceExternalId = StringUtils.EMPTY;
    private ZonedDateTime eventDate = ZonedDateTime.now(ZoneOffset.UTC);
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(5);

    private QueuePaymentEventFixture() {
    }

    public static QueuePaymentEventFixture aQueuePaymentEventFixture() {
        return new QueuePaymentEventFixture();
    }

    public QueuePaymentEventFixture withResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public QueuePaymentEventFixture withResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }

    public QueuePaymentEventFixture withParentResourceExternalId(String parentResourceExternalId) {
        this.parentResourceExternalId = parentResourceExternalId;
        return this;
    }

    public QueuePaymentEventFixture withEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public QueuePaymentEventFixture withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public QueuePaymentEventFixture withEventData(String eventData) {
        this.eventData = eventData;
        return this;
    }

    public QueuePaymentEventFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public QueuePaymentEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "PAYMENT_CREATED":
                var externalMetadata = new JsonObject();
                externalMetadata.addProperty("key1", "value1");

                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("amount", 1000)
                                .put("description", "a description")
                                .put("language", "en")
                                .put("reference", "aref")
                                .put("return_url", "https://example.org")
                                .put("gateway_account_id", gatewayAccountId)
                                .put("payment_provider", "sandbox")
                                .put("delayed_capture", false)
                                //.put("external_metadata", externalMetadata)   //todo: will be enabled after changes to connector
                                .build());
                break;
            case "PAYMENT_DETAILS_ENTERED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("email", "j.doe@example.org")
                                .put("last_digits_card_number", "4242")
                                .put("first_digits_card_number", "424242")
                                .put("cardholder_name", "J citizen")
                                .put("expiry_date", "11/21")
                                .put("address_line1", "12 Rouge Avenue")
                                .put("address_postcode", "N1 3QU")
                                .put("address_city", "London")
                                .put("address_country", "GB")
                                .put("card_brand", "visa")
                                .put("gateway_transaction_id", gatewayAccountId)
                                .put("corporate_surcharge", 5)
                                .put("total_amount", 1005)
                                .build());
                break;
            case "CAPTURE_CONFIRMED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("gateway_event_date", eventDate.toString())
                                .put("fee", 5)
                                .put("net_amount", 1069)
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

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    @Override
    public QueuePaymentEventFixture insert(AmazonSQS sqsClient) {
        this.sqsMessageId = QueueEventFixtureUtil.insert(sqsClient, eventType, eventDate, resourceExternalId,
                parentResourceExternalId, resourceType, eventData);
        return this;
    }

    public PactDslJsonBody getAsPact() {
       return QueueEventFixtureUtil.getAsPact(eventType, eventDate, resourceExternalId,
               parentResourceExternalId, resourceType, eventData);
    }
}
