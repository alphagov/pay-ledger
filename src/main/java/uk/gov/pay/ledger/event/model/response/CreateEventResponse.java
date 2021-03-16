package uk.gov.pay.ledger.event.model.response;

import java.util.Optional;

public class CreateEventResponse {
    public enum CreateEventState {
        INSERTED,
        IGNORED,
        ERROR
    }

    private boolean isSuccessful;
    private CreateEventState state;
    private Exception exception;

    public CreateEventResponse(Optional<Long> status) {
        this.isSuccessful = true;
        this.state = status.isPresent() ? CreateEventState.INSERTED : CreateEventState.IGNORED;
    }

    public CreateEventResponse(Exception exception) {
        this.exception = exception;
        this.isSuccessful = false;
        this.state = CreateEventState.ERROR;
    }

    private CreateEventResponse(CreateEventState state) {
        this.isSuccessful = true;
        this.state = state;
    }

    public static CreateEventResponse ignoredEventResponse() {
        return new CreateEventResponse(CreateEventState.IGNORED);
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public CreateEventState getState() {
        return state;
    }

    public String getErrorMessage() {
        return exception != null ? exception.getMessage() : "";
    }
}
