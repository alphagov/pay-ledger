package uk.gov.pay.ledger.util;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

public class DisputeReasonMapperTest {

    @Test
    public void shouldReturnUnrecognised() {
        String mappedValue = DisputeReasonMapper.mapToApi("unrecognized");
        assertThat(mappedValue, is("unrecognised"));
    }

    @Test
    public void shouldReturnOther() {
        String mappedValue = DisputeReasonMapper.mapToApi("insufficient_funds");
        assertThat(mappedValue, is("other"));
    }

    @Test
    public void shouldHandleNullValue() {
        String mappedValue = DisputeReasonMapper.mapToApi(null);
        assertThat(mappedValue, is(emptyString()));
    }

    @Test
    public void shouldHandleEmptyValue() {
        String mappedValue = DisputeReasonMapper.mapToApi("");
        assertThat(mappedValue, is(emptyString()));
    }
}