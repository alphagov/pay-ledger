package uk.gov.pay.ledger.transaction.search.common;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;

public class TransactionSearchParamsValidatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    TransactionSearchParams searchParams;

    @Before
    public void setup(){
        searchParams = new TransactionSearchParams();
        searchParams.setAccountId("account_id");
    }
    @Test
    public void shouldThrowException_whenAccountIdIsNull() {
        searchParams.setAccountId(null);
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Field [account_id] cannot be empty");
        TransactionSearchParamsValidator.validateSearchParams(searchParams);
    }

    @Test
    public void shouldThrowException_whenInvalidFromDate() {
        searchParams.setFromDate("wrong-date");
        thrown.expect(UnparsableDateException.class);
        thrown.expectMessage("Input from_date (wrong-date) is wrong format");
        TransactionSearchParamsValidator.validateSearchParams(searchParams);
    }

    @Test
    public void shouldThrowException_whenInvalidToDate() {
        searchParams.setToDate("wrong-date");
        thrown.expect(UnparsableDateException.class);
        thrown.expectMessage("Input to_date (wrong-date) is wrong format");
        TransactionSearchParamsValidator.validateSearchParams(searchParams);
    }

    @Test
    public void shouldNotThrowException_whenValidDateFormats() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParams(searchParams);
    }
}