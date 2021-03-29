package uk.gov.pay.ledger.exception;

import io.dropwizard.jersey.validation.JerseyViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.UNPROCESSABLE_ENTITY_422;

public class JerseyViolationExceptionMapper implements ExceptionMapper<JerseyViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyViolationExceptionMapper.class);

    @Override
    public Response toResponse(JerseyViolationException exception) {
        LOGGER.error(exception.getConstraintViolations().iterator().next().getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorIdentifier.GENERIC,
                exception.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
        return Response.status(UNPROCESSABLE_ENTITY_422).entity(errorResponse).type(APPLICATION_JSON).build();
    }
}
