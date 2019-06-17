package uk.gov.pay.ledger.exception;

public class ValidationException extends BadRequestException {

    public ValidationException(String message) {
        super(message);
    }
}
