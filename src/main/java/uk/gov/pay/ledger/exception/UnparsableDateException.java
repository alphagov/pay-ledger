package uk.gov.pay.ledger.exception;

import static java.lang.String.format;

public class UnparsableDateException extends BadRequestException {
    public UnparsableDateException(String fieldName, String value) {
        super(format("Input %s (%s) is wrong format", fieldName, value));
    }
}
