package uk.gov.pay.ledger.util;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

public class ZonedDateTimeTimestampMatcher extends TypeSafeMatcher<Timestamp> {

    private Timestamp from;

    private ZonedDateTimeTimestampMatcher(Timestamp from) {
        this.from = from;
    }

    public static ZonedDateTimeTimestampMatcher isDate(ZonedDateTime zonedDateTime) {
        return new ZonedDateTimeTimestampMatcher(Timestamp.from(zonedDateTime.toInstant()));
    }

    @Override
    protected boolean matchesSafely(Timestamp to) {
        return Matchers.is(from).matches(to);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("A timestamp: " + from);
    }
}
