package uk.gov.pay.ledger.transaction.search.common;

import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParamsValidator {

    private static final String GATEWAY_ACCOUNT_ID = "account_id";
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";

    public static void validateSearchParams(TransactionSearchParams searchParams) {

        throwIfBlankFieldValue(GATEWAY_ACCOUNT_ID, searchParams.getAccountId());

        if (isNotBlank(searchParams.getFromDate())) {
            validateDate(FROM_DATE_FIELD, searchParams.getFromDate());
        }
        if (isNotBlank(searchParams.getToDate())) {
            validateDate(TO_DATE_FIELD, searchParams.getToDate());
        }
    }

    private static void validateDate(String fieldName, String dateToParse) {
        try {
            ZonedDateTime.parse(dateToParse);
        } catch (DateTimeParseException e) {
            throw new UnparsableDateException(fieldName, dateToParse);
        }
    }

    private static void throwIfBlankFieldValue(String fieldName, String value) {
        if (isBlank(value))
            throw new ValidationException(format("Field [%s] cannot be empty", fieldName));
    }
}
