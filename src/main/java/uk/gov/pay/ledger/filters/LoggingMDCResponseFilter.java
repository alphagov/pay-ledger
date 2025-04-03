package uk.gov.pay.ledger.filters;

import org.slf4j.MDC;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.util.List;

import static uk.gov.pay.ledger.filters.LoggingMDCRequestFilter.PARENT_TRANSACTION_EXTERNAL_ID;
import static uk.gov.pay.ledger.filters.LoggingMDCRequestFilter.TRANSACTION_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.AGREEMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.LEDGER_EVENT_ID;

public class LoggingMDCResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        List.of(LEDGER_EVENT_ID, TRANSACTION_EXTERNAL_ID, PARENT_TRANSACTION_EXTERNAL_ID, AGREEMENT_EXTERNAL_ID).forEach(MDC::remove);
    }
}
