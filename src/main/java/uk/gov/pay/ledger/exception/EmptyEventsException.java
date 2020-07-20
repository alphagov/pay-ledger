package uk.gov.pay.ledger.exception;

public class EmptyEventsException extends RuntimeException {
    public EmptyEventsException(String message) {
        super(message);
    }
}
