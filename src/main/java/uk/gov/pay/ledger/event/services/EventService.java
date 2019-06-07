package uk.gov.pay.ledger.event.services;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;

import java.util.Optional;

public class EventService {

    private EventDao eventDao;

    @Inject
    public EventService(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    public CreateEventResponse createIfDoesNotExist(Event event) {
        try {
            Optional<Long> status = eventDao.insertEventIfDoesNotExistWithResourceTypeId(event);
            return new CreateEventResponse(status);
        } catch (Exception e) {
            return new CreateEventResponse(e);
        }
    }

    public class CreateEventResponse {
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

    public enum CreateEventState {
        INSERTED,
        IGNORED,
        ERROR
    }
}
