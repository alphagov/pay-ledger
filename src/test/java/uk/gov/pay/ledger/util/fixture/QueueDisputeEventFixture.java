package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.pay.ledger.event.entity.EventEntity;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZonedDateTime;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;

public class QueueDisputeEventFixture implements QueueFixture<QueueDisputeEventFixture, EventEntity> {
    private String sqsMessageId;
    private String serviceId = randomAlphanumeric(10);
    private Boolean live = true;
    private ResourceType resourceType = DISPUTE;
    private String resourceExternalId = randomAlphanumeric(20);
    private String parentResourceExternalId = randomAlphanumeric(26);
    private ZonedDateTime eventDate = ZonedDateTime.parse("2022-02-07T08:46:01.123456Z");
    private String eventType = "DISPUTE_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";
    private String gatewayAccountId = randomAlphanumeric(20);

    private QueueDisputeEventFixture() {
    }

    public static QueueDisputeEventFixture aQueueDisputeEventFixture() {
        return new QueueDisputeEventFixture();
    }

    public QueueDisputeEventFixture withResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }

    public QueueDisputeEventFixture withEventDate(ZonedDateTime eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public QueueDisputeEventFixture withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public QueueDisputeEventFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public QueueDisputeEventFixture withEventData(String eventData) {
        this.eventData = eventData;
        return this;
    }

    public QueueDisputeEventFixture withLive(Boolean live) {
        this.live = live;
        return this;
    }

    public QueueDisputeEventFixture withParentResourceExternalId(String parentResourceExternalId) {
        this.parentResourceExternalId = parentResourceExternalId;
        return this;
    }

    public QueueDisputeEventFixture withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public QueueDisputeEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "DISPUTE_CREATED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("gateway_account_id", "a-gateway-account-id")
                                .put("amount", 6500)
                                .put("reason", "duplicate")
                                .put("evidence_due_date", "2022-02-14T23:59:59.000000Z")
                                .build());
                break;
            case "DISPUTE_LOST":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("fee", 1500)
                                .put("amount", 6500)
                                .put("gateway_account_id", "a-gateway-account-id")
                                .build());
                break;
            default:
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.of("event_data", "event_data"));
        }
        return this;
    }

    @Override
    public EventEntity toEntity() {
        return new EventEntity(0L, sqsMessageId, serviceId, live, resourceType, resourceExternalId, parentResourceExternalId, eventDate, eventType, eventData, false);
    }

    @Override
    public QueueDisputeEventFixture insert(SqsClient sqsClient) {
        this.sqsMessageId = QueueEventFixtureUtil.insert(sqsClient, eventType, eventDate, serviceId, live, resourceExternalId,
                parentResourceExternalId, resourceType, eventData);
        return this;
    }

    public PactDslJsonBody getAsPact() {
        return QueueEventFixtureUtil.getAsPact(serviceId, live, eventType, eventDate, resourceExternalId,
                parentResourceExternalId, resourceType, eventData);
    }
}
