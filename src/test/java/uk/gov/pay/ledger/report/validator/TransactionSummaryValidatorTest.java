package uk.gov.pay.ledger.report.validator;

import org.junit.jupiter.api.Test;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransactionSummaryValidatorTest {

    private TransactionSummaryParams transactionSummaryParams;

    @Test
    public void shouldThrowException_whenFromDateIsMissingAndValidationIsFalse() {
        transactionSummaryParams = new TransactionSummaryParams();

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, false));
        assertThat(validationException.getMessage(), is("Field [from_date] can not be null"));
    }

    @Test
    public void shouldNotThrowException_whenFromDateIsMissingAndValidationIsTrue() {
        transactionSummaryParams = new TransactionSummaryParams();
        TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, true);
    }
}