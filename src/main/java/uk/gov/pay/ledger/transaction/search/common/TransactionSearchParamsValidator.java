package uk.gov.pay.ledger.transaction.search.common;

import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParamsValidator {
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";

    public static void validateSearchParams(TransactionSearchParams searchParams, CommaDelimitedSetParameter gatewayAccountIds) {
        if (isNotBlank(searchParams.getFromDate())) {
            validateDate(FROM_DATE_FIELD, searchParams.getFromDate());
        }
        if (isNotBlank(searchParams.getToDate())) {
            validateDate(TO_DATE_FIELD, searchParams.getToDate());
        }
        if(searchParams.getWithParentTransaction() && gatewayAccountIds.getParameters().isEmpty()){
            throw new ValidationException("gateway_account_id is mandatory to search with parent transaction");
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
