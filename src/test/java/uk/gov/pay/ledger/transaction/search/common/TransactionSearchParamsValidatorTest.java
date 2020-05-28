package uk.gov.pay.ledger.transaction.search.common;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

@RunWith(JUnitParamsRunner.class)
public class TransactionSearchParamsValidatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    TransactionSearchParams searchParams;

    @Before
    public void setup() {
        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldThrowException_whenInvalidFromDate() {
        searchParams.setFromDate("wrong-date");
        thrown.expect(UnparsableDateException.class);
        thrown.expectMessage("Input from_date (wrong-date) is wrong format");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    public void shouldThrowException_whenInvalidToDate() {
        searchParams.setToDate("wrong-date");
        thrown.expect(UnparsableDateException.class);
        thrown.expectMessage("Input to_date (wrong-date) is wrong format");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    public void shouldNotThrowException_whenValidDateFormats() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter("account-id"));
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdsAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        thrown.expect(ValidationException.class);
        thrown.expectMessage("gateway_account_id is mandatory to search with parent transaction");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter(""));
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdIsNotAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter("1,2"));
    }

    @Test
    public void validateSearchParamsForCsvShouldNotThrowExceptionForValidParams() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1,2"));
    }

    @Test(expected = ValidationException.class)
    public void validateSearchParamsForCsvShouldThrowExceptionIfGatewayAccountIsNotAvailable() {
        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter(""));

        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, null);
    }

    @Parameters({
            "wrong-date, 2019-05-01T10:15:30Z",
            "2019-05-01T10:15:30Z, wrong-date"
    })
    @Test(expected = UnparsableDateException.class)
    public void validateSearchParamsForCsvShouldThrowExceptionForInvalidDates(String fromDate, String toDate) {
        searchParams.setFromDate(fromDate);
        searchParams.setToDate(toDate);

        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1"));
    }
}