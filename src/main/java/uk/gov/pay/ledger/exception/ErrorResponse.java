package uk.gov.pay.ledger.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("error_identifier")
    @Schema(example = "GENERIC")
    private final ErrorIdentifier identifier;

    @JsonProperty("message")
    @ArraySchema(schema = @Schema(example = "example error message"))
    private final List<String> messages;

    public ErrorResponse(ErrorIdentifier identifier, List<String> messages) {
        this.identifier = identifier;
        this.messages = messages;
    }

    public ErrorResponse(ErrorIdentifier identifier, String message) {
        this(identifier, List.of(message));
    }

    public ErrorIdentifier getIdentifier() {
        return identifier;
    }

    public List<String> getMessages() {
        return messages;
    }
}
