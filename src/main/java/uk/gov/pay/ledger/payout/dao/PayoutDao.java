package uk.gov.pay.ledger.payout.dao;

import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.payout.dao.mapper.PayoutMapper;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;

import java.util.Optional;

public class PayoutDao {

    private final String SELECT_PAYOUT_BY_GATEWAY_PAYOUT_ID = "SELECT * FROM payout " +
            "WHERE gateway_payout_id = :gatewayPayoutId";

    private final String UPSERT_PAYOUT = "INSERT INTO payout " +
            "(" +
            "gateway_payout_id,\n" +
            "amount,\n" +
            "paid_out_date,\n" +
            "statement_descriptor,\n" +
            "status,\n" +
            "type,\n" +
            "event_count,\n" +
            "payout_details\n" +
            ")\n " +
            "VALUES (" +
            ":gateway_payout_id, " +
            ":amount, " +
            ":paid_out_date, " +
            ":statement_descriptor, " +
            ":status, " +
            ":type, " +
            ":event_count, " +
            "CAST(:payout_details as jsonb) " +
            ") " +
            "ON CONFLICT (gateway_payout_id) DO UPDATE SET " +
            "gateway_payout_id = EXCLUDED.gateway_payout_id, " +
            "amount = EXCLUDED.amount, " +
            "paid_out_date = EXCLUDED.paid_out_date, " +
            "statement_descriptor = EXCLUDED.statement_descriptor, " +
            "status = EXCLUDED.status, " +
            "type = EXCLUDED.type, " +
            "event_count = EXCLUDED.event_count, " +
            "payout_details = EXCLUDED.payout_details";

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
                        .bind("gateway_payout_id", payout.getGatewayPayoutId())
                        .bind("amount", payout.getAmount())
                        .bind("paid_out_date", payout.getPaidOutDate())
                        .bind("statement_descriptor", payout.getStatementDescriptor())
                        .bind("status", payout.getStatus())
                        .bind("type", payout.getType())
                        .bind("event_count", payout.getEventCount())
                        .bind("payout_details", payout.getPayoutDetails())
                        .execute());
    }

}
