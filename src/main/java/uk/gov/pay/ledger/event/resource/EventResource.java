package uk.gov.pay.ledger.event.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.exception.ErrorResponse;
import uk.gov.pay.ledger.queue.EventMessage;
import uk.gov.pay.ledger.queue.EventMessageDto;
import uk.gov.pay.ledger.queue.EventMessageHandler;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/event")
@Produces(APPLICATION_JSON)
@Tag(name = "Events")
public class EventResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventResource.class);
    private final EventDao eventDao;
    private final EventMessageHandler eventMessageHandler;

    @Inject
    public EventResource(EventDao eventDao, EventMessageHandler eventMessageHandler) {
        this.eventDao = eventDao;
        this.eventMessageHandler = eventMessageHandler;
    }

    @Path("/{eventId}")
    @GET
    @Timed
    @Operation(
            summary = "Find event by ID (id of event table)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Event.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Event getEvent(@PathParam("eventId") Long eventId) {
        LOGGER.info("Get event request: {}", eventId);
        return eventDao.getById(eventId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @POST
    @Timed
    public Response writeEvent(List<EventMessageDto> events) throws QueueException {
        try {
            eventMessageHandler.processEventBatch(events
                    .stream()
                    .map(eventMessageDto -> EventMessage.of(eventMessageDto, null))
                    .collect(Collectors.toList())
            );
        } catch (Exception ignored) {
            LOGGER.warn("Failed to process batch of events");
            throw ignored;
        }
        return Response.accepted().build();
    }

    @Path("/ticker")
    @GET
    @Timed
    @Operation(
            operationId = "listEvents",
            summary = "Get list of events between a date/time range",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventTicker.class)))),
                    @ApiResponse(responseCode = "422", description = "Missing query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Invalid parameters or Downstream system error")
            }
    )
    public List<EventTicker> eventTickerList(@NotEmpty @Parameter(description = "from date of events to be searched (this date is inclusive).", required = true, example = "\"2015-08-14T12:35:00Z\"")
                                             @QueryParam("from_date") String fromDate,
                                             @NotEmpty @Parameter(description = "to date of events to be searched (this date is exclusive)", required = true, example = "\"2015-08-14T12:35:00Z\"")
                                             @QueryParam("to_date") String toDate) {
        return eventDao.findEventsTickerFromDate(ZonedDateTime.parse(fromDate), ZonedDateTime.parse(toDate));
    }
}