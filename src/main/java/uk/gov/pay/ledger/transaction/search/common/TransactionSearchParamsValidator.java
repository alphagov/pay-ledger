package uk.gov.pay.ledger.transaction.search.common;

import uk.gov.pay.commons.validation.DateTimeUtils;
import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TransactionSearchParamsValidator {
    private static final String FROM_DATE_FIELD = "from_date";
    private static final String TO_DATE_FIELD = "to_date";
    private static final String FROM_SETTLED_DATE_FIELD = "from_settled_date";
    private static final String TO_SETTLED_DATE_FIELD = "to_settled_date";

    public static void validateSearchParams(TransactionSearchParams searchParams, CommaDelimitedSetParameter gatewayAccountIds) {
        validateDates(searchParams);
    }

    public static void validateSearchParamsForCsv(TransactionSearchParams searchParams, CommaDelimitedSetParameter gatewayAccountIds) {
        validateDates(searchParams);

        if (isEmpty(gatewayAccountIds)) {
            throw new ValidationException("gateway_account_id is mandatory to search transactions for CSV");
        }
    }

    private static boolean isEmpty(CommaDelimitedSetParameter gatewayAccountIds) {
        return gatewayAccountIds == null || gatewayAccountIds.getParameters().isEmpty();
    }

    private static void validateDates(TransactionSearchParams searchParams) {
        if (isNotBlank(searchParams.getFromDate())) {
            validateDate(FROM_DATE_FIELD, searchParams.getFromDate());
        }
        if (isNotBlank(searchParams.getToDate())) {
            validateDate(TO_DATE_FIELD, searchParams.getToDate());
        }
        if (isNotBlank(searchParams.getFromSettledDate())) {
            validSettledDate(FROM_SETTLED_DATE_FIELD, searchParams.getFromSettledDate());
        }
        if (isNotBlank(searchParams.getToSettledDate())) {
            validSettledDate(TO_SETTLED_DATE_FIELD, searchParams.getToSettledDate());
        }
    }

    private static void validateDate(String fieldName, String dateToParse) {
        try {
            ZonedDateTime.parse(dateToParse);
        } catch (DateTimeParseException e) {
            throw new UnparsableDateException(fieldName, dateToParse);
        }
    }

    private static void validSettledDate(String fieldName, String settledDateToParse) {
        if (!DateTimeUtils.fromLocalDateOnlyString(settledDateToParse).isPresent()) {
            throw new UnparsableDateException(fieldName, settledDateToParse);
        }
    }
}
