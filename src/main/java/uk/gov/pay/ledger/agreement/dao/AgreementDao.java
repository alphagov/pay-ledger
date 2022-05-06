package uk.gov.pay.ledger.agreement.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.resource.AgreementSearchParams;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class AgreementDao {
    private static final String SELECT_AGREEMENT_WITH_PAYMENT_INSTRUMENT =
            "SELECT a.*," +
            "pi.external_id as p_external_id," +
            "pi.agreement_external_id as p_agreement_external_id," +
            "pi.email as p_email," +
            "pi.cardholder_name as p_cardholder_name," +
            "pi.address_line1 as p_address_line1," +
            "pi.address_line2 as p_address_line2," +
            "pi.address_postcode as p_address_postcode," +
            "pi.address_city as p_address_city," +
            "pi.address_county as p_address_county," +
            "pi.address_country as p_address_country," +
            "pi.last_digits_card_number as p_last_digits_card_number," +
            "pi.expiry_date as p_expiry_date," +
            "pi.card_brand as p_card_brand," +
            "pi.event_count as p_event_count," +
            "pi.created_date as p_created_date " +
            "FROM agreement a LEFT JOIN (" +
            "SELECT DISTINCT ON (agreement_external_id) * FROM payment_instrument ORDER BY agreement_external_id, created_date DESC" +
            ") pi ON pi.agreement_external_id = a.external_id ";

    private static final String FIND_BY_EXTERNAL_ID =
            SELECT_AGREEMENT_WITH_PAYMENT_INSTRUMENT +
            "WHERE a.external_id = :externalId";

    private final String UPSERT_AGREEMENT = "INSERT INTO agreement " +
            "(" +
            "external_id," +
            "gateway_account_id, " +
            "service_id," +
            "live," +
            "reference," +
            "description," +
            "status," +
            "created_date," +
            "event_count" +
            ") " +
            "VALUES (" +
            ":externalId, " +
            ":gatewayAccountId, " +
            ":serviceId, " +
            ":live, " +
            ":reference, " +
            ":description, " +
            ":status, " +
            ":createdDate, " +
            ":eventCount" +
            ") " +
            "ON CONFLICT (external_id) DO UPDATE SET " +
            "external_id = EXCLUDED.external_id, " +
            "gateway_account_id = EXCLUDED.gateway_account_id, " +
            "service_id = EXCLUDED.service_id, " +
            "live = EXCLUDED.live, " +
            "reference = EXCLUDED.reference, " +
            "description = EXCLUDED.description, " +
            "status = EXCLUDED.status, " +
            "created_date = EXCLUDED.created_date, " +
            "event_count = EXCLUDED.event_count " +
            "WHERE EXCLUDED.event_count >= agreement.event_count";

    private static final String SEARCH_AGREEMENT =
            SELECT_AGREEMENT_WITH_PAYMENT_INSTRUMENT +
                    ":searchExtraFields " +
                    "ORDER BY a.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String COUNT_AGREEEMENT = "SELECT count(1) " +
            "FROM agreement a " +
            ":searchExtraFields";

    private final Jdbi jdbi;

    @Inject
    public AgreementDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<AgreementEntity> findByExternalId(String externalId) {
        return jdbi.withHandle(handle -> handle.createQuery(FIND_BY_EXTERNAL_ID)
                .bind("externalId", externalId)
                .map(new AgreementMapper())
                .findOne());
    }

    public void upsert(AgreementEntity agreement) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_AGREEMENT)
                        .bindBean(agreement)
                        .execute());
    }

    public List<AgreementEntity> searchAgreements(AgreementSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), SEARCH_AGREEMENT));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("offset", searchParams.getOffset());
            query.bind("limit", searchParams.getDisplaySize());
            return query
                    .map(new AgreementMapper())
                    .list();
        });
    }

    public Long getTotalForSearch(AgreementSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), COUNT_AGREEEMENT));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            return query
                    .mapTo(Long.class)
                    .one();
        });
    }

    private String createSearchTemplate(List<String> filterTemplates, String baseQueryString) {
        String searchClauseTemplate = String.join(" AND ", filterTemplates);
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?
                "WHERE " + searchClauseTemplate :
                "";

        return baseQueryString.replace(
                ":searchExtraFields",
                searchClauseTemplate);
    }

    private BiConsumer<String, Object> bindSearchParameter(Query query) {
        return (searchKey, searchValue) -> {
            if (searchValue instanceof List<?>) {
                query.bindList(searchKey, ((List<?>) searchValue));
            } else {
                query.bind(searchKey, searchValue);
            }
        };
    }
}