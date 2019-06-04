package uk.gov.pay.ledger.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import io.dropwizard.setup.Environment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class HealthCheckResource {

    private Environment environment;

    @Inject
    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    public Set<Map.Entry<String, HealthCheck.Result>> healthCheck() {
        return environment.healthChecks().runHealthChecks().entrySet();

    }
}
