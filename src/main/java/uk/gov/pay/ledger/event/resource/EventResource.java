package uk.gov.pay.ledger.event.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.exception.ValidationException;
import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.time.ZonedDateTime;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path("/v1/event")
@Produces(APPLICATION_JSON)
public class EventResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventResource.class);
    private final EventDao eventDao;

    @Inject
    public EventResource(EventDao eventDao) {
        this.eventDao = eventDao;
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
    public List<EventTicker> eventTickerList(@QueryParam("from_date") String fromDate) {
        if(isBlank(fromDate)) {
            throw new ValidationException("from_date is mandatory to receive event ticker");
        }

        return eventDao.findEventsTickerFromDate(ZonedDateTime.parse(fromDate));
    }
}
