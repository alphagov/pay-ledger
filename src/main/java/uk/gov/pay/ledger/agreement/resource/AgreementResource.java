package uk.gov.pay.ledger.agreement.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.agreement.model.Agreement;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.payout.model.PayoutSearchResponse;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;
import uk.gov.pay.ledger.payout.service.PayoutService;
import uk.gov.pay.ledger.transaction.resource.TransactionResource;
import uk.gov.pay.ledger.transaction.service.AccountIdListSupplierManager;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/agreement")
@Produces(APPLICATION_JSON)
public class AgreementResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgreementResource.class);
    private AgreementService agreementService;
    private EventService eventService;

    @Inject
    public AgreementResource(AgreementService agreementService, EventService eventService) {
        this.agreementService = agreementService;
        this.eventService = eventService;
    }

    @Path("/")
    @GET
    @Timed
    public AgreementSearchResponse search(@BeanParam AgreementSearchParams querySearchParams,
                                          @Context UriInfo uriInfo) {
        var searchParams = Optional.ofNullable(querySearchParams).orElse(new AgreementSearchParams());
        return agreementService.searchAgreements(searchParams, uriInfo);
    }

    @Path("/")
    @GET
    @Produces("text/csv; qs=.5")
    @Timed
    public Response searchCSV(@BeanParam AgreementSearchParams querySearchParams,
                                          @Context UriInfo uriInfo) {
        StreamingOutput stream = outputStream -> {

        };
        return Response.ok(stream).build();
    }

    @Path("/{agreementExternalId}")
    @GET
    @Timed
    public Agreement get(@PathParam("agreementExternalId") String agreementExternalId,
                            @HeaderParam("X-Consistent") Boolean consistent,
                            @Context UriInfo uriInfo) {
        return agreementService.findAgreementEntity(agreementExternalId)
                .map(agreementEntity -> {
                    if (consistent != null && consistent) {
                        // ensure this entity is up to date
                        var count = eventService.countByResourceExternalId(agreementExternalId);
                        if (Objects.equals(count, agreementEntity.getEventCount())) {
                            // projection was made with the latest number of events, return that
                            LOGGER.info("X-Consistent set, projection was up to date");
                            return Agreement.from(agreementEntity);
                        } else {

                            // projection was made but we've received new events since, return a projection from the event stream
                            var stream = eventService.getEventDigestForResourceId(agreementExternalId);

                            // for now this doesn't actually upsert the projection -- it would be fine to call the same code that upserts
                            // but would need to think through what would happen if two things are competing to do that (with EXCLUDED rules it should be fine)

                            // note the dependant payment instrument projection would need to be streamed and projected too, omitted that for now
                            var entity = agreementService.inMemoryCalculateAgreementFor(stream);
                            LOGGER.info("X-Consistent set, projection out of date and was projected");
                            return Agreement.from(entity);
                        }
                    } else {
                        // it doesn't matter if our projection is behind
                        LOGGER.info("X-Consistent not set");
                        return Agreement.from(agreementEntity);
                    }
                })
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}