package uk.gov.pay.ledger.payout.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.dao.PayoutDao;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.model.PayoutEntityFactory;
import uk.gov.pay.ledger.payout.model.PayoutSearchResponse;
import uk.gov.pay.ledger.payout.model.PayoutView;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;
import uk.gov.pay.ledger.util.pagination.PaginationBuilder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

public class PayoutService {

    private final PayoutDao payoutDao;
    private final PayoutEntityFactory payoutEntityFactory;

    @Inject
    public PayoutService(PayoutDao payoutDao, PayoutEntityFactory payoutEntityFactory) {
        this.payoutDao = payoutDao;
        this.payoutEntityFactory = payoutEntityFactory;
    }

    public void upsertPayoutFor(EventDigest eventDigest) {
        PayoutEntity payoutEntity = payoutEntityFactory.create(eventDigest);
        payoutDao.upsert(payoutEntity);
    }

    public PayoutSearchResponse searchPayouts(List<String> gatewayAccountIds, PayoutSearchParams searchParams, UriInfo uriInfo) {
        if (!gatewayAccountIds.isEmpty()) {
            searchParams.setGatewayAccountIds(gatewayAccountIds);
        }

        List<PayoutView> payoutViewList = payoutDao.searchPayouts(searchParams)
                .stream()
                .map(PayoutView::from)
                .collect(Collectors.toList());

        Long total = payoutDao.getTotalForSearch(searchParams);

        long size = searchParams.getDisplaySize();
        if (total > 0 && searchParams.getDisplaySize() > 0) {
            long lastPage = (total + size - 1) / size;
            if (searchParams.getPageNumber() > lastPage || searchParams.getPageNumber() < 1) {
                throw new WebApplicationException("The requested page was not found",
                        Response.Status.NOT_FOUND);
            }
        }

        PaginationBuilder paginationBuilder = new PaginationBuilder(searchParams, uriInfo)
                .withTotalCount(total)
                .buildResponse();


        return new PayoutSearchResponse(total, payoutViewList.size(),
                searchParams.getPageNumber(), payoutViewList)
                .withPaginationBuilder(paginationBuilder);
    }

    public PayoutSearchResponse searchPayouts(PayoutSearchParams searchParams, UriInfo uriInfo) {
        return searchPayouts(List.of(), searchParams, uriInfo);
    }
}
