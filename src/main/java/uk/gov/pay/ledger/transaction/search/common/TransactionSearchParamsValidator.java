package uk.gov.pay.ledger.transaction.search.common;

import uk.gov.pay.ledger.exception.UnparsableDateException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParamsValidator {
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";

    public static void validateSearchParams(TransactionSearchParams searchParams) {
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
}
