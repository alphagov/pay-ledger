package uk.gov.pay.ledger.queue;

public class QueueException extends Exception {

    public QueueException() {

    }

    public QueueException(String message, Exception e) {
        super(message, e);
    }
}
