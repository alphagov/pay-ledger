package uk.gov.pay.ledger.transaction.search.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionSearchParamsValidatorTest {

    private TransactionSearchParams searchParams;

    @BeforeEach
    void setup() {
        searchParams = new TransactionSearchParams();
    }

    @Test
    void shouldThrowException_whenInvalidFromDate() {
        searchParams.setFromDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input from_date (wrong-date) is wrong format"));
    }

    @Test
    void shouldThrowException_whenInvalidToDate() {
        searchParams.setToDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input to_date (wrong-date) is wrong format"));
    }

    @Test
    void shouldThrowException_whenInvalidFromSettledDate() {
        searchParams.setFromSettledDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input from_settled_date (wrong-date) is wrong format"));
    }

    @Test
    void shouldThrowException_whenInvalidToSettledDate() {
        searchParams.setToSettledDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input to_settled_date (wrong-date) is wrong format"));
    }

    @Test
    void shouldNotThrowException_whenValidDateFormats() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    void shouldNotThrowException_whenValidSettledDateFormats() {
        searchParams.setFromSettledDate("2020-09-10");
        searchParams.setToSettledDate("2020-09-10");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    void validateSearchParamsForCsvShouldNotThrowExceptionForValidParams() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1,2"));
    }

    @Test
    void validateSearchParamsForCsvShouldThrowExceptionIfGatewayAccountIsNotAvailable() {
        assertThrows(ValidationException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, null));
    }

    @Test
    void validateSearchParamsForCsvShouldThrowExceptionIfGatewayAccountIdsIsEmptyString() {
        assertThrows(ValidationException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("")));
    }

    @ParameterizedTest
    @CsvSource({
            "wrong-date, 2019-05-01T10:15:30Z",
            "2019-05-01T10:15:30Z, wrong-date"
    })
    void validateSearchParamsForCsvShouldThrowExceptionForInvalidDates(String fromDate, String toDate) {
        searchParams.setFromDate(fromDate);
        searchParams.setToDate(toDate);

        assertThrows(UnparsableDateException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1")));
    }

    @Test
    void validateFromSettledDateSearchParamsShouldThrowExceptionForInvalidDates() {
        searchParams.setFromSettledDate("25/09/2020");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input from_settled_date (25/09/2020) is wrong format"));
    }

    @Test
    void validateToSettledDateSearchParamsShouldThrowExceptionForInvalidDates() {
        searchParams.setToSettledDate("2020.09.25");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input to_settled_date (2020.09.25) is wrong format"));
    }
}