package uk.gov.pay.ledger.transaction.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.model.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/transaction")
@Produces(APPLICATION_JSON)
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);
    private final TransactionDao transactionDao;

    @Inject
    public TransactionResource(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Path("/{gatewayAccountId}")
    @GET
    @Timed
    public List<Transaction> getByGatewayAccountId(@PathParam("gatewayAccountId") String gatewayAccountId) {
        LOGGER.info("Get transaction request: {}", gatewayAccountId);
        return transactionDao.getByGatewayAccountId(gatewayAccountId);
    }
}
