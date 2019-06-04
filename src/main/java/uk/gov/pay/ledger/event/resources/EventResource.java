package uk.gov.pay.ledger.event.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
}
