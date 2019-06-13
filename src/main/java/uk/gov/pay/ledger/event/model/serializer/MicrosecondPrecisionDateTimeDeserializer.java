package uk.gov.pay.ledger.event.model.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class MicrosecondPrecisionDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    public static final DateTimeFormatter MICROSECOND_FORMATTER =
            MicrosecondPrecisionDateTimeSerializer.MICROSECOND_FORMATTER;

    public MicrosecondPrecisionDateTimeDeserializer() {
        this(null);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            return ZonedDateTime.from(MICROSECOND_FORMATTER.parse(p.getText()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MicrosecondPrecisionDateTimeDeserializer(Class<ZonedDateTime> t) {
        super(t);
    }
}
