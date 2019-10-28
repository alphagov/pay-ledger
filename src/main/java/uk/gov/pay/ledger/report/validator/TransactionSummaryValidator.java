package uk.gov.pay.ledger.report.validator;

import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;

import static org.eclipse.jetty.util.StringUtil.isBlank;

public class TransactionSummaryValidator {
    public static void validateTransactionSummaryParams(TransactionSummaryParams params, Boolean overrideFromDateValidation) {
        if (!overrideFromDateValidation && isBlank(params.getFromDate())) {
            throw new ValidationException("Field [from_date] can not be null");
        }
    }
}
