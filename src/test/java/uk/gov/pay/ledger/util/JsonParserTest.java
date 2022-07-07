package uk.gov.pay.ledger.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class JsonParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void safeGetAsLong_shouldReturnValueAsLong() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("field1", 10L));
        JsonNode jsonNode = objectMapper.readTree(data);

        Long value = JsonParser.safeGetAsLong(jsonNode, "field1");

        assertThat(value, is(10L));
    }

    @Test
    public void safeGetAsLong_shouldReturnNullWhenFieldDoesNotExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("somefield", 10L));
        JsonNode jsonNode = objectMapper.readTree(data);

        Long value = JsonParser.safeGetAsLong(jsonNode, "field1");

        assertThat(value, is(nullValue()));
    }

    @Test
    public void safeGetAsBoolean_shouldReturnValueIfFieldExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("field1", false));
        JsonNode jsonNode = objectMapper.readTree(data);

        Boolean value = JsonParser.safeGetAsBoolean(jsonNode, "field1", false);

        assertThat(value, is(false));
    }

    @Test
    public void safeGetAsBoolean_shouldReturnDefaultValueIfFieldDoesNotExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("somefield", true));
        JsonNode jsonNode = objectMapper.readTree(data);

        Boolean value = JsonParser.safeGetAsBoolean(jsonNode, "field1", true);

        assertThat(value, is(true));
    }

    @Test
    public void safeGetAsString_shouldReturnValueIfFieldExists() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("field1", "value1"));
        JsonNode jsonNode = objectMapper.readTree(data);

        String value = JsonParser.safeGetAsString(jsonNode, "field1");

        assertThat(value, is("value1"));
    }

    @Test
    public void safeGetAsString_shouldReturnNullWhenFieldDoesNotExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("somefield", 10L));
        JsonNode jsonNode = objectMapper.readTree(data);

        String value = JsonParser.safeGetAsString(jsonNode, "field1");

        assertThat(value, is(nullValue()));
    }

    @Test
    public void safeGetAsDate_shouldReturnValueIfFieldExists() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("field1", "2018-03-12T16:25:01.123Z"));
        JsonNode jsonNode = objectMapper.readTree(data);

        ZonedDateTime value = JsonParser.safeGetAsDate(jsonNode, "field1");

        assertThat(value.toString(), is("2018-03-12T16:25:01.123Z"));
    }

    @Test
    public void safeGetAsDate_shouldReturnNullWhenFieldDoesNotExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("somefield", "2018-03-12T16:25:01.123456Z"));
        JsonNode jsonNode = objectMapper.readTree(data);

        ZonedDateTime value = JsonParser.safeGetAsDate(jsonNode, "field1");

        assertThat(value, is(nullValue()));
    }

    @Test
    public void safeGetEpochLongAsDate_shouldReturnValueIfFieldExists() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("epoch_long", 1652223599L));
        JsonNode jsonNode = objectMapper.readTree(data);

        ZonedDateTime value = JsonParser.safeGetEpochLongAsDate(jsonNode, "epoch_long");

        assertThat(value.toString(), is("2022-05-10T22:59:59Z"));
    }

    @Test
    public void safeGetEpochLongAsDate_shouldReturnNullWhenFieldDoesNotExist() throws IOException {
        String data = new GsonBuilder().create()
                .toJson(ImmutableMap.of("somefield", 1652223599L));
        JsonNode jsonNode = objectMapper.readTree(data);

        ZonedDateTime value = JsonParser.safeGetEpochLongAsDate(jsonNode, "epoch_long");

        assertThat(value, is(nullValue()));
    }
}
