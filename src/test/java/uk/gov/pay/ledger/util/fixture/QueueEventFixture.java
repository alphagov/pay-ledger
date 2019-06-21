package uk.gov.pay.ledger.util.fixture;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.rule.SqsTestDocker;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class QueueEventFixture implements QueueFixture<QueueEventFixture, Event> {
    private String sqsMessageId;
    private ResourceType resourceType = ResourceType.CHARGE;
    private String resourceExternalId = RandomStringUtils.randomAlphanumeric(20);
    private ZonedDateTime eventDate = ZonedDateTime.now(ZoneOffset.UTC);
    private String eventType = "PAYMENT_CREATED";
    private String eventData = "{\"event_data\": \"event data\"}";

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

    @Override
    public Event toEntity() {
        return new Event(0L, sqsMessageId, resourceType, resourceExternalId, eventDate, eventType, eventData);
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
                        "\"event_type\":\"%s\"," +
                        "\"resource_type\": \"%s\"," +
                        "\"event_details\": %s" +
                        "}",
                eventDate.toString(),
                resourceExternalId,
                eventType,
                resourceType.toString().toLowerCase(),
                eventData
                );

        SendMessageResult result = sqsClient.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), messageBody);
        this.sqsMessageId = result.getMessageId();
        return this;
    }
}
