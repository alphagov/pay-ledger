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
import uk.gov.pay.ledger.event.model.EventTicker;
import uk.gov.pay.ledger.exception.ErrorResponse;
import uk.gov.pay.ledger.queue.EventMessage;
import uk.gov.pay.ledger.queue.EventMessageDto;
import uk.gov.pay.ledger.queue.EventMessageHandler;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.logstash.logback.argument.StructuredArguments.kv;

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

    
    @POST
    @Timed
    @Operation(
            operationId = "writeEvent",
            summary = "Write a list of events to the ledger database",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventTicker.class)))),
                    @ApiResponse(responseCode = "422", description = "Missing query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Invalid parameters or Downstream system error")
            }
    )
    public Response writeEvent(@Valid List<EventMessageDto> events) throws QueueException {
        try {
            eventMessageHandler.processEventBatch(events
                    .stream()
                    .map(eventMessageDto -> EventMessage.of(eventMessageDto, null))
                    .collect(Collectors.toUnmodifiableList())
            );
        } catch (Exception exception) {
            var ids = events.stream()
                    .map(EventMessageDto::getExternalId)
                    .collect(Collectors.toUnmodifiableList());
            LOGGER.warn(String.format("Failed to process batch of events"),
                    kv("resource_external_ids", ids),
                    kv("message", exception.getMessage())
            );
            throw exception;
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
