package uk.gov.pay.ledger.event.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.queue.EventMessage;
import uk.gov.pay.ledger.queue.EventMessageDto;
import uk.gov.pay.ledger.queue.EventMessageHandler;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.time.ZonedDateTime;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path("/v1/event")
@Produces(APPLICATION_JSON)
public class EventResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventResource.class);
    private final EventDao eventDao;
    private final EventMessageHandler eventMessageHandler;

    @Inject
    public EventResource(EventDao eventDao, EventMessageHandler eventMessageHandler) {
        this.eventDao = eventDao;
        this.eventMessageHandler = eventMessageHandler;
    }

    @POST
    @Timed
    public Event writeEvent(EventMessageDto eventMessageDto) {
        return eventMessageHandler.handle(EventMessage.of(eventMessageDto));
    }

    @Path("/{eventId}")
    @GET
    @Timed
    public Event getEvent(@PathParam("eventId") Long eventId) {
        LOGGER.info("Get event request: {}", eventId);
        return eventDao.getById(eventId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Path("/ticker")
    @GET
    @Timed
    public List<EventTicker> eventTickerList(@NotEmpty @QueryParam("from_date") String fromDate, @NotEmpty @QueryParam("to_date") String toDate) {
        return eventDao.findEventsTickerFromDate(ZonedDateTime.parse(fromDate), ZonedDateTime.parse(toDate));
    }
}