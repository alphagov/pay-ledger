package uk.gov.pay.ledger.filters;

import org.slf4j.MDC;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.util.Optional;

import static uk.gov.service.payments.logging.LoggingKeys.AGREEMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.LEDGER_EVENT_ID;

public class LoggingMDCRequestFilter implements ContainerRequestFilter {

    public static final String PARENT_TRANSACTION_EXTERNAL_ID = "parent_transaction_external_id";
    public static final String TRANSACTION_EXTERNAL_ID = "transaction_external_id";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        getPathParameterFromRequest("eventId", requestContext)
                .ifPresent(eventId -> MDC.put(LEDGER_EVENT_ID, eventId));

        getPathParameterFromRequest("transactionExternalId", requestContext)
                .ifPresent(transactionExternalId -> MDC.put(TRANSACTION_EXTERNAL_ID, transactionExternalId));

        getPathParameterFromRequest("parentTransactionExternalId", requestContext)
                .ifPresent(parentTransactionExternalId -> MDC.put(PARENT_TRANSACTION_EXTERNAL_ID, parentTransactionExternalId));

        getPathParameterFromRequest("agreementExternalId", requestContext)
                .ifPresent(agreementExternalId -> MDC.put(AGREEMENT_EXTERNAL_ID, agreementExternalId));
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getPathParameters().getFirst(parameterName));
    }
}
