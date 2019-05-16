package uk.gov.pay.ledger.event;

import com.codahale.metrics.annotation.Timed;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path("/event")
@Produces(APPLICATION_JSON)
public class EventResource {
    private final EventDao eventDao;

    @Inject
    public EventResource(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    @Path("/{eventId}")
    @GET
    @Timed
    public Event getEvent(@PathParam("eventId") String eventId) {
        return eventDao.getById(eventId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}
