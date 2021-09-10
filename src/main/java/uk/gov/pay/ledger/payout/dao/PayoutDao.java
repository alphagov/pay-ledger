package uk.gov.pay.ledger.payout.dao;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import uk.gov.pay.ledger.payout.dao.mapper.PayoutMapper;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.search.PayoutSearchParams;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class PayoutDao {

    private final String SELECT_PAYOUT_BY_GATEWAY_PAYOUT_ID = "SELECT * FROM payout " +
            "WHERE gateway_payout_id = :gatewayPayoutId";

    private static final String SEARCH_PAYOUTS = "SELECT * FROM payout p " +
            ":searchExtraFields " +
            "ORDER BY p.created_date DESC OFFSET :offset LIMIT :limit";

    private static final String COUNT_PAYOUTS = "SELECT count(1) " +
            "FROM payout p " +
            ":searchExtraFields ";

    private final String UPSERT_PAYOUT = "INSERT INTO payout " +
            "(" +
            "gateway_payout_id," +
            "service_id," +
            "live," +
            "amount," +
            "paid_out_date," +
            "state," +
            "event_count," +
            "payout_details," +
            "created_date," +
            "gateway_account_id" +
            ") " +
            "VALUES (" +
            ":gatewayPayoutId, " +
            ":serviceId, " +
            ":live, " +
            ":amount, " +
            ":paidOutDate, " +
            ":state, " +
            ":eventCount, " +
            "CAST(:payoutDetails as jsonb), " +
            ":createdDate, " +
            ":gatewayAccountId " +
            ") " +
            "ON CONFLICT (gateway_payout_id) DO UPDATE SET " +
            "gateway_payout_id = EXCLUDED.gateway_payout_id, " +
            "service_id = EXCLUDED.service_id, " +
            "live = EXCLUDED.live, " +
            "amount = EXCLUDED.amount, " +
            "paid_out_date = EXCLUDED.paid_out_date, " +
            "state = EXCLUDED.state, " +
            "event_count = EXCLUDED.event_count, " +
            "payout_details = EXCLUDED.payout_details, " +
            "created_date = EXCLUDED.created_date, " +
            "gateway_account_id = EXCLUDED.gateway_account_id " +
            "WHERE EXCLUDED.event_count >= payout.event_count";

    private Jdbi jdbi;

    @Inject
    public PayoutDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<PayoutEntity> findByGatewayPayoutId(String gatewayPayoutId) {
        return jdbi.withHandle(handle -> handle.createQuery(SELECT_PAYOUT_BY_GATEWAY_PAYOUT_ID)
                .bind("gatewayPayoutId", gatewayPayoutId)
                .map(new PayoutMapper())
                .findFirst());
    }

    public void upsert(PayoutEntity payout) {
        jdbi.withHandle(handle ->
                handle.createUpdate(UPSERT_PAYOUT)
                        .bindBean(payout)
                        .execute());
    }

    public List<PayoutEntity> searchPayouts(PayoutSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), SEARCH_PAYOUTS));
            searchParams.getQueryMap().forEach(bindSearchParameter(query));
            query.bind("offset", searchParams.getOffset());
            query.bind("limit", searchParams.getDisplaySize());
            return query
                    .map(new PayoutMapper())
                    .list();
        });
    }

    public Long getTotalForSearch(PayoutSearchParams searchParams) {
        return jdbi.withHandle(handle -> {
            Query query = handle.createQuery(createSearchTemplate(searchParams.getFilterTemplates(), COUNT_PAYOUTS));
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
