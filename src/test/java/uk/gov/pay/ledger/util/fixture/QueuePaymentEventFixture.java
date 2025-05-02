package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.service.payments.commons.model.Source;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.service.payments.commons.model.Source.CARD_API;

public class QueuePaymentEventFixture implements QueueFixture<QueuePaymentEventFixture, EventEntity> {
    private String sqsMessageId;
    private String serviceId = "a-service-id";
    private Boolean live;
    private ResourceType resourceType = ResourceType.PAYMENT;
    private String resourceExternalId = "a-resource-external-id";
    private String parentResourceExternalId = StringUtils.EMPTY;
    private ZonedDateTime eventDate = ZonedDateTime.parse("2018-03-12T16:25:01.123456Z");
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";
    private String gatewayAccountId = "a-gateway-account-id";
    private String credentialExternalId = "a-credentials-external-id";
    private Source source;
    private Map<String, Object> metadata = new HashMap<>();
    private boolean includeMetadata = true;
    private boolean reprojectDomainObject;
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private String gatewayTransactionId = "a-provider-transaction-id";

    private QueuePaymentEventFixture() {
    }

    public static QueuePaymentEventFixture aQueuePaymentEventFixture() {
        return new QueuePaymentEventFixture();
    }

    public QueuePaymentEventFixture withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public QueuePaymentEventFixture withLive(Boolean live) {
        this.live = live;
        return this;
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

    public QueuePaymentEventFixture withCredentialExternalId(String credentialExternalId) {
        this.credentialExternalId = credentialExternalId;
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

    public QueuePaymentEventFixture includeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
        return this;
    }


    public QueuePaymentEventFixture withIsReprojectDomainObject(boolean reprojectDomainObject) {
        this.reprojectDomainObject = reprojectDomainObject;
        return this;
    }

    public QueuePaymentEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "PAYMENT_CREATED":
                if (metadata.isEmpty() && includeMetadata) {
                    metadata.put("key", "value");
                }
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("amount", 1000)
                                .put("description", "This is a description")
                                .put("language", "en")
                                .put("reference", "This is a reference")
                                .put("return_url", "http://return.invalid")
                                .put("gateway_account_id", gatewayAccountId)
                                .put("credential_external_id", credentialExternalId)
                                .put("payment_provider", "sandbox")
                                .put("delayed_capture", false)
                                .put("moto", false)
                                .put("external_metadata", metadata)
                                .put("email", "test@email.invalid")
                                .put("cardholder_name", "Mr Test")
                                .put("address_line1", "125 Kingsway")
                                .put("address_postcode", "WC2B 6NH")
                                .put("address_city", "London")
                                .put("source", CARD_API)
                                .put("address_country", "GB")
                                .put("authorisation_mode", "web")
                                .put("agreement_id", "an-agreement-external-id")
                                .build());
                break;
            case "PAYMENT_DETAILS_ENTERED":
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("last_digits_card_number", "4242")
                                .put("first_digits_card_number", "424242")
                                .put("cardholder_name", "Mr Test")
                                .put("expiry_date", "12/99")
                                .put("address_line1", "125 Kingsway")
                                .put("address_postcode", "WC2B 6NH")
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
            case "PAYMENT_DETAILS_SUBMITTED_BY_API":
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("last_digits_card_number", "4242")
                                .put("first_digits_card_number", "424242")
                                .put("cardholder_name", "Mr Test")
                                .put("expiry_date", "12/99")
                                .put("card_type", "DEBIT")
                                .put("card_brand", "visa")
                                .put("card_brand_label", "Visa")
                                .put("gateway_transaction_id", "gateway_transaction_id")
                                .build());
                break;
            case "CAPTURE_CONFIRMED":
            case "STATUS_CORRECTED_TO_CAPTURED_TO_MATCH_GATEWAY_STATUS":
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
                externalMetadata.addProperty("auth_code", "012345");
                externalMetadata.addProperty("status", "success");
                eventData = gsonBuilder.create()
                    .toJson(ImmutableMap.builder()
                            .put("credential_external_id", credentialExternalId)
                            .put("amount", 1000)
                            .put("description", "This is a description")
                            .put("reference", "This is a reference")
                            .put("email", "j.doe@example.org")
                            .put("external_metadata", externalMetadata)
                            .put("last_digits_card_number", "4242")
                            .put("first_digits_card_number", "424242")
                            .put("cardholder_name", "Mr Test")
                            .put("expiry_date", "12/99")
                            .put("card_brand", "visa")
                            .put("card_brand_label", "Visa")
                            .put("payment_provider", "sandbox")
                            .put("gateway_transaction_id", "providerId")
                            .build());
                break;
            case "CANCELLED_BY_USER":
                eventData = gsonBuilder.create().toJson(Map.of("gateway_transaction_id", "gateway_transaction_id"));
                break;
            case "USER_EMAIL_COLLECTED":
                eventData = gsonBuilder.create().toJson(Map.of("email", "test@example.org"));
                break;
            case "REQUESTED_3DS_EXEMPTION":
                eventData = gsonBuilder.create().toJson(Map.of("exemption_3ds_requested", "OPTIMISED"));
                break;
            case "GATEWAY_3DS_EXEMPTION_RESULT_OBTAINED":
                eventData = gsonBuilder.create().toJson(Map.of("exemption3ds", "EXEMPTION_HONOURED"));
                break;
            case "GATEWAY_3DS_INFO_OBTAINED":
                eventData = gsonBuilder.create().toJson(Map.of("version_3ds", "2.1.0"));
                break;
            case "GATEWAY_REQUIRES_3DS_AUTHORISATION":
                eventData = gsonBuilder.create().toJson(Map.of("version_3ds", "2.1.0", "requires_3ds", true));
                break;
            case "GATEWAY_DOES_NOT_REQUIRE_3DS_AUTHORISATION":
                eventData = gsonBuilder.create().toJson(Map.of("requires_3ds", false));
                break;
            case "PAYMENT_DETAILS_TAKEN_FROM_PAYMENT_INSTRUMENT":
                eventData = gsonBuilder.create()
                        .toJson(ImmutableMap.builder()
                                .put("last_digits_card_number", "1234")
                                .put("first_digits_card_number", "123456")
                                .put("cardholder_name", "Test")
                                .put("expiry_date", "11/99")
                                .put("address_line1", "10 WCB")
                                .put("address_postcode", "E1 8XX")
                                .put("address_city", "London")
                                .put("address_county", "London")
                                .put("address_country", "UK")
                                .put("card_type", "DEBIT")
                                .put("card_brand", "visa")
                                .put("card_brand_label", "Visa")
                                .put("gateway_transaction_id", gatewayTransactionId)
                                .build());
                break;
            default:
                eventData = gsonBuilder.create().toJson(Map.of("event_data", "event_data"));
        }
        return this;
    }

    @Override
    public EventEntity toEntity() {
        return new EventEntity(0L, sqsMessageId, serviceId, live, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType, eventData, reprojectDomainObject);
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
    public QueuePaymentEventFixture insert(SqsClient sqsClient) {
        this.sqsMessageId = QueueEventFixtureUtil.insert(sqsClient, eventType, eventDate, serviceId, live, resourceExternalId,
                parentResourceExternalId, resourceType, eventData);
        return this;
    }

    public PactDslJsonBody getAsPact() {
        return QueueEventFixtureUtil.getAsPact(serviceId, live, eventType, eventDate, resourceExternalId,
                parentResourceExternalId, resourceType, eventData);
    }
}
