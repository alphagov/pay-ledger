package uk.gov.pay.ledger.agreement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.gov.pay.ledger.agreement.dao.AgreementDao;
import uk.gov.pay.ledger.agreement.dao.PaymentInstrumentDao;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.AgreementsFactory;
import uk.gov.pay.ledger.agreement.model.Agreement;
import uk.gov.pay.ledger.agreement.model.AgreementSearchResponse;
import uk.gov.pay.ledger.agreement.resource.AgreementSearchParams;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.service.EventService;
import uk.gov.pay.ledger.exception.EmptyEventsException;
import uk.gov.pay.ledger.util.pagination.PaginationBuilder;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AgreementService {
    private final AgreementDao agreementDao;
    private final PaymentInstrumentDao paymentInstrumentDao;
    private final AgreementsFactory agreementEntityFactory;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    @Inject
    public AgreementService(AgreementDao agreementDao,
                            PaymentInstrumentDao paymentInstrumentDao,
                            AgreementsFactory agreementsFactory,
                            EventService eventService,
                            ObjectMapper objectMapper) {
        this.agreementDao = agreementDao;
        this.paymentInstrumentDao = paymentInstrumentDao;
        this.agreementEntityFactory = agreementsFactory;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    public Optional<Agreement> findAgreement(String externalId) {
        return agreementDao.findByExternalId(externalId)
                .map(Agreement::from);
    }

    public Optional<AgreementEntity> findAgreementEntity(
            String externalId,
            boolean isConsistent
    ) {
        return findAgreementEntity(externalId, isConsistent, null, null);
    }

    public Optional<AgreementEntity> findAgreementEntity(
            String externalId,
            boolean isConsistent,
            @Nullable String accountId,
            @Nullable String serviceId
    ) {
        if (isConsistent) {
            EventDigest eventDigest;
            try {
                eventDigest = eventService.getEventDigestForResource(externalId);
            }
            catch (EmptyEventsException e) {
                return Optional.empty();
            }

            return Optional.of(agreementDao.findByExternalId(externalId)
                    .filter(projectedAgreementEntity -> databaseProjectionSnapshotIsUpToDateWithEventStream(projectedAgreementEntity, eventDigest))
                    .orElse(projectAgreement(eventDigest)))
                    .filter(agreement -> accountId == null || agreement.getGatewayAccountId().equals(accountId))
                    .filter(agreement -> serviceId == null || agreement.getServiceId().equals(serviceId));
        } else {
            return agreementDao.findByExternalId(externalId)
                    .filter(agreement -> accountId == null || agreement.getGatewayAccountId().equals(accountId))
                    .filter(agreement -> serviceId == null || agreement.getServiceId().equals(serviceId));
        }
    }

    public AgreementEntity projectAgreement(EventDigest eventDigest) {
        return agreementEntityFactory.create(eventDigest);
    }

    public void upsertAgreementFor(EventDigest eventDigest) {
        agreementDao.upsert(projectAgreement(eventDigest));
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

    public List<Event> findEvents(String agreementExternalId, String serviceId) {
        if (findAgreementEntity(agreementExternalId, true, null, serviceId).isEmpty()) {
            throw new WebApplicationException(format("Agreement with id [%s] not found", agreementExternalId), Response.Status.NOT_FOUND);
        }

        var events = agreementDao.findAssociatedEvents(agreementExternalId);

        return events.stream().map(event -> Event.from(event, objectMapper)).collect(Collectors.toList());
    }

    private boolean databaseProjectionSnapshotIsUpToDateWithEventStream(AgreementEntity agreementEntity, EventDigest eventDigest) {
        return eventDigest.getEventCount() <= agreementEntity.getEventCount();
    }
}
