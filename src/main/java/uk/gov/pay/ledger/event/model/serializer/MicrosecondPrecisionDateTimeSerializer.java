package uk.gov.pay.ledger.event.model.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class MicrosecondPrecisionDateTimeSerializer extends StdSerializer<ZonedDateTime>  {

    public static final DateTimeFormatter MICROSECOND_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendInstant(6)
                    .toFormatter(Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    public MicrosecondPrecisionDateTimeSerializer() {
        this(null);
    }

    private MicrosecondPrecisionDateTimeSerializer(Class<ZonedDateTime> t) {
        super(t);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider sp) throws IOException {
        gen.writeString(MICROSECOND_FORMATTER.format(value));
    }
}
