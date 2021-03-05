package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.commons.model.Source;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.pay.commons.model.Source.CARD_API;

public class QueuePaymentEventFixture implements QueueFixture<QueuePaymentEventFixture, Event> {
    private String sqsMessageId;
    private ResourceType resourceType = ResourceType.PAYMENT;
    private String resourceExternalId = RandomStringUtils.randomAlphanumeric(20);
    private String parentResourceExternalId = StringUtils.EMPTY;
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(5);
    private Source source;
    private Map<String, Object> metadata = new HashMap<>();
    private boolean includeMetada = true;
    private GsonBuilder gsonBuilder = new GsonBuilder();

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

    public QueuePaymentEventFixture withSource(Source source) {
        this.source = source;
        return this;
    }

    public QueuePaymentEventFixture withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public QueuePaymentEventFixture includeMetadata(boolean includeMetada) {
        this.includeMetada = includeMetada;
        return this;
    }

    public QueuePaymentEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "PAYMENT_CREATED":
                if (metadata.isEmpty() && includeMetada) {
                    metadata.put("key", "value");
                }
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("amount", 1000)
                                .put("description", "a description")
                                .put("language", "en")
                                .put("reference", "aref")
                                .put("return_url", "https://example.org")
                                .put("gateway_account_id", gatewayAccountId)
                                .put("payment_provider", "sandbox")
                                .put("delayed_capture", false)
                                .put("moto", false)
                                .put("live", true)
                                .put("external_metadata", metadata)
                                .put("email", "j.doe@example.org")
                                .put("cardholder_name", "J citizen")
                                .put("address_line1", "12 Rouge Avenue")
                                .put("address_postcode", "N1 3QU")
                                .put("address_city", "London")
                                .put("source", CARD_API)
                                .put("address_country", "GB")
                                .build());
                break;
            case "PAYMENT_DETAILS_ENTERED":
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("last_digits_card_number", "4242")
                                .put("first_digits_card_number", "424242")
                                .put("cardholder_name", "J citizen")
                                .put("expiry_date", "11/21")
                                .put("address_line1", "12 Rouge Avenue")
                                .put("address_postcode", "N1 3QU")
                                .put("address_city", "London")
                                .put("address_country", "GB")
                                .put("card_type", "DEBIT")
                                .put("card_brand", "visa")
                                .put("card_brand_label", "Visa")
                                .put("gateway_transaction_id", gatewayAccountId)
                                .put("corporate_surcharge", 5)
                                .put("total_amount", 1005)
                                .build());
                break;
            case "CAPTURE_CONFIRMED":
                eventData = gsonBuilder.create()
                        .toJson(Map.of("gateway_event_date", eventDate.toString(),
                                "captured_date", eventDate.toString(),
                                "fee", 5,
                                "net_amount", 1069));
                break;
            case "CAPTURE_SUBMITTED":
                eventData = gsonBuilder.create().toJson(Map.of("capture_submitted_date", eventDate.toString()));
                break;
            case "PAYMENT_NOTIFICATION_CREATED":
                var externalMetadata = new JsonObject();
                externalMetadata.addProperty("telephone_number", "+447700900796");
                externalMetadata.addProperty("processor_id", "processorId");
                externalMetadata.addProperty("authorised_date", "2018-02-21T16:05:33Z");
                externalMetadata.addProperty("created_date", "2018-02-21T15:05:13Z");
                externalMetadata.addProperty("auth_code", "authCode");
                externalMetadata.addProperty("status", "success");
                eventData = gsonBuilder.create()
                    .toJson(ImmutableMap.builder()
                            .put("amount", 1000)
                            .put("description", "New passport application")
                            .put("reference", "MRPC12345")
                            .put("email", "j.doe@example.org")
                            .put("external_metadata", externalMetadata)
                            .put("last_digits_card_number", "4242")
                            .put("first_digits_card_number", "424242")
                            .put("cardholder_name", "J citizen")
                            .put("expiry_date", "11/21")
                            .put("card_brand", "visa")
                            .put("card_brand_label", "Visa")
                            .put("payment_provider", "sandbox")
                            .put("gateway_transaction_id", "providerId")
                            .build());
                break;
            case "CANCELLED_BY_USER":
                eventData = gsonBuilder.create().toJson(Map.of("gateway_transaction_id", "validGatewayTransactionId"));
                break;
            case "USER_EMAIL_COLLECTED":
                eventData = gsonBuilder.create().toJson(Map.of("email", "test@example.org"));
                break;
            case "GATEWAY_3DS_EXEMPTION_RESULT_OBTAINED":
                eventData = gsonBuilder.create().toJson(Map.of("exemption3ds", "HONOURED"));
                break;
            default:
                eventData = gsonBuilder.create().toJson(Map.of("event_data", "event_data"));
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
