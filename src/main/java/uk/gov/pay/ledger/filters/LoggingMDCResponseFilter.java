package uk.gov.pay.ledger.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class LoggingMDCResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        MDC.clear();
    }
}
