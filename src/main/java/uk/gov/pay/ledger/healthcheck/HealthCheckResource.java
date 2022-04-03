package uk.gov.pay.ledger.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Path("/")
@Tag(name = "Other")
public class HealthCheckResource {

    private Environment environment;

    @Inject
    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Healthcheck endpoint the ledger (checks postgresql, sqs queue)",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(example = "{" +
                    "    \"postgresql\": {" +
                    "        \"healthy\": true," +
                    "        \"message\": \"Healthy\"" +
                    "    }," +
                    "    \"sqsQueue\": {" +
                    "        \"healthy\": true," +
                    "        \"message\": \"Healthy\"" +
                    "    }," +
                    "    \"deadlocks\": {" +
                    "        \"healthy\": true," +
                    "        \"message\": \"Healthy\"" +
                    "    }" +
                    "}")
            )),
                    @ApiResponse(responseCode = "503", description = "Service Unavailable")
            }
    )
    public Response healthCheck() {
        SortedMap<String, HealthCheck.Result> results = environment.healthChecks().runHealthChecks();

        Map<String, Map<String, Object>> response = results.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        healthCheck -> ImmutableMap.of(
                                "healthy", healthCheck.getValue().isHealthy(),
                                "message", defaultString(healthCheck.getValue().getMessage(), "Healthy"))
                        )
                );

        Response.ResponseBuilder res = allHealthy(results.values()) ? Response.ok() : status(503);

        return res.entity(response).build();
    }

    private boolean allHealthy(Collection<HealthCheck.Result> results) {
        return results.stream().allMatch(HealthCheck.Result::isHealthy);
    }
}
