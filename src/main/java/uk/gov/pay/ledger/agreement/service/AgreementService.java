package uk.gov.pay.ledger.agreement.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntityFactory;
import uk.gov.pay.ledger.agreement.model.Agreement;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.resource.AgreementSearchParams;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.util.pagination.PaginationBuilder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import java.util.stream.Collectors;

public class AgreementService {
    private final AgreementDao agreementDao;
    private final PaymentInstrumentDao paymentInstrumentDao;
    private final AgreementEntityFactory agreementEntityFactory;

    @Inject
    public AgreementService(AgreementDao agreementDao, PaymentInstrumentDao paymentInstrumentDao, AgreementEntityFactory agreementEntityFactory) {
        this.agreementDao = agreementDao;
        this.paymentInstrumentDao = paymentInstrumentDao;
        this.agreementEntityFactory = agreementEntityFactory;
    }

    public Optional<Agreement> findAgreement(String externalId) {
        return agreementDao.findByExternalId(externalId)
                .map(Agreement::from);
    }

    public void upsertAgreementFor(EventDigest eventDigest) {
        var entity = agreementEntityFactory.create(eventDigest);
        agreementDao.upsert(entity);
    }

    public void upsertPaymentInstrumentFor(EventDigest eventDigest) {
        var entity = agreementEntityFactory.createPaymentInstrument(eventDigest);
        paymentInstrumentDao.upsert(entity);
    }

    public AgreementSearchResponse searchAgreements(AgreementSearchParams searchParams, UriInfo uriInfo) {
        var agreements = agreementDao.searchAgreements(searchParams)
                .stream()
                .map(Agreement::from)
                .collect(Collectors.toUnmodifiableList());
        var total = agreementDao.getTotalForSearch(searchParams);

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

        return new AgreementSearchResponse(total, agreements.size(),
                searchParams.getPageNumber(), agreements)
                .withPaginationBuilder(paginationBuilder);
    }
}