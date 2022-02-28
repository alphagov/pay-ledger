package uk.gov.pay.ledger.util.fixture;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;

import java.time.ZonedDateTime;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.pay.ledger.event.model.ResourceType.DISPUTE;

public class QueueDisputeEventFixture implements QueueFixture<QueueDisputeEventFixture, Event> {
    private String sqsMessageId;
    private String serviceId = randomAlphanumeric(10);;
    private Boolean live = true;
    private ResourceType resourceType = DISPUTE;
    private String resourceExternalId = randomAlphanumeric(20);;
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

    public QueueDisputeEventFixture withDefaultEventDataForEventType(String eventType) {
        switch (eventType) {
            case "DISPUTE_CREATED":
                eventData = new GsonBuilder().create()
                        .toJson(ImmutableMap.builder()
                                .put("fee", 1500)
                                .put("evidence_due_date", 1644883199)
                                .put("gateway_account_id", gatewayAccountId)
                                .put("amount", 6500)
                                .put("net_amount", 8000)
                                .put("reason", "duplicate")
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
        return new Event(0L, sqsMessageId, serviceId, live, resourceType, resourceExternalId, EMPTY, eventDate, eventType, eventData, false);
    }

    @Override
    public QueueDisputeEventFixture insert(AmazonSQS sqsClient) {
        this.sqsMessageId = QueueEventFixtureUtil.insert(sqsClient, eventType, eventDate, serviceId, live, resourceExternalId,
                EMPTY, resourceType, eventData);
        return this;
    }

    public PactDslJsonBody getAsPact() {
        return QueueEventFixtureUtil.getAsPact(serviceId, live, eventType, eventDate, resourceExternalId,
                EMPTY, resourceType, eventData);
    }
}
