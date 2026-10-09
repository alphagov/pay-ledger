package uk.gov.pay.ledger.transaction.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeeTypeTest {

    @ParameterizedTest
    @CsvSource({
            "transaction, TRANSACTION, Fee (transaction)",
            "fraud_protection, FRAUD_PROTECTION, Fee (fraud protection)",
            "radar, RADAR, Fee (fraud protection)",
            "three_ds, THREE_DS, Fee (3DS)",
            "gateway, GATEWAY, Fee (gateway)"
    })
    void fromNameValueShouldReturnKnownFeeType(String name, FeeType expectedFeeType, String expectedCsvHeaderName) {
        Optional<FeeType> result = FeeType.fromName(name);

        assertTrue(result.isPresent());
        assertEquals(expectedFeeType, result.get());
        assertEquals(expectedCsvHeaderName, result.get().getCsvFieldName());
    }


    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({"unknown"})
    void fromNameValueShouldReturnEmptyForNullAndEmptyType(String name) {
        Optional<FeeType> result = FeeType.fromName(name);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
