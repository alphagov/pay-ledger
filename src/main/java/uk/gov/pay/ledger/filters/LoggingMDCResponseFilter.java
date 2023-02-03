package uk.gov.pay.ledger.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
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
