package uk.gov.pay.ledger.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRequestException.class);

    @Override
    public Response toResponse(BadRequestException exception) {
        LOGGER.error(exception.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorIdentifier.GENERIC, exception.getMessage());
        return Response.status(400).entity(errorResponse).type(APPLICATION_JSON).build();
    }
}
