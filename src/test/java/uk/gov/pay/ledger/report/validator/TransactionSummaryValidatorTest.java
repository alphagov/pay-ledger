package uk.gov.pay.ledger.report.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;

public class TransactionSummaryValidatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TransactionSummaryParams transactionSummaryParams;

    @Test
    public void shouldThrowException_whenFromDateIsMissingAndValidationIsFalse() {
        transactionSummaryParams = new TransactionSummaryParams();
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Field [from_date] can not be null");
        TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, false);
    }

    @Test
    public void shouldNotThrowException_whenFromDateIsMissingAndValidationIsTrue() {
        transactionSummaryParams = new TransactionSummaryParams();
        TransactionSummaryValidator.validateTransactionSummaryParams(transactionSummaryParams, true);
    }
}