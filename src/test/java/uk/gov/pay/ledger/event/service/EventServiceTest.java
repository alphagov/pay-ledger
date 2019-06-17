package uk.gov.pay.ledger.event.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.model.response.CreateEventResponse;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    @Mock
    private EventDao mockEventDao;

    private EventService eventService;

    private Event event;

    @Before
    public void setUp() {
        eventService = new EventService(mockEventDao);
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulCreatedResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.of(1L));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.INSERTED));
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulIgnoredResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.empty());

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.IGNORED));
    }

    @Test
    public void createIfDoesNotExistReturnsNotSuccessfulResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event))
                .thenThrow(new RuntimeException("forced failure"));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertFalse(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.ERROR));
        assertThat(response.getErrorMessage(), is("forced failure"));
    }
}