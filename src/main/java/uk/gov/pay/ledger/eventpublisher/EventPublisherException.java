package uk.gov.pay.ledger.eventpublisher;

public class EventPublisherException extends Exception {

    public EventPublisherException(String message, Exception e) {
        super(message, e);
    }
}
