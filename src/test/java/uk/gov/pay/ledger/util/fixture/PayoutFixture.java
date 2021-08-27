package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.state.PayoutState;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static uk.gov.pay.ledger.payout.entity.PayoutEntity.PayoutEntityBuilder.aPayoutEntity;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.PayoutFixtureBuilder.aPayoutFixture;

public class PayoutFixture implements DbFixture<PayoutFixture, PayoutEntity> {

    private Long id;
    private String gatewayPayoutId;
    private Long amount;
    private ZonedDateTime createdDate = now(UTC);
    private ZonedDateTime paidOutDate;
    private PayoutState state;
    private Integer eventCount;
    private String payoutDetails;
    private String gatewayAccountId;

    @Override
    public PayoutFixture insert(Jdbi jdbi) {
        jdbi.withHandle(handle ->
                handle.execute(
                        "INSERT INTO" +
                                " payout(\n" +
                                "   id,\n" +
                                "   gateway_payout_id,\n" +
                                "   amount,\n" +
                                "   created_date,\n" +
                                "   paid_out_date,\n" +
                                "   state,\n" +
                                "   event_count,\n" +
                                "   payout_details,\n" +
                                "   gateway_account_id\n" +
                                " )\n" +
                                " VALUES (?,?,?,?,?,?,?,CAST(? as jsonb),?)",
                        id, gatewayPayoutId, amount, createdDate, paidOutDate,
                        state, eventCount, payoutDetails, gatewayAccountId
                ));
        return this;
    }

    @Override
    public PayoutEntity toEntity() {
        return aPayoutEntity()
                .withAmount(amount)
                .withCreatedDate(createdDate)
                .withGatewayPayoutId(gatewayPayoutId)
                .withId(id)
                .withPaidOutDate(paidOutDate)
                .withState(state)
                .withEventCount(eventCount)
                .withPayoutDetails(payoutDetails)
                .withGatewayAccountId(gatewayAccountId)
                .build();
    }

    public static List<PayoutEntity> aPayoutList(String gatewayAccountId, int noOfPayouts) {
        List<PayoutEntity> payoutEntityList = new ArrayList<>();

        for (int i = 0; i < noOfPayouts; i++) {
            payoutEntityList.add(aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .build()
                    .toEntity());
        }
        return payoutEntityList;
    }

    public static List<PayoutEntity> aPersistedPayoutList(String gatewayAccountId, int noOfPayouts, Jdbi jdbi) {
        List<PayoutEntity> payoutEntityList = new ArrayList<>();
        for (int i = 0; i < noOfPayouts; i++) {
            PayoutEntity entity = aPayoutFixture()
                    .withGatewayAccountId(gatewayAccountId)
                    .build()
                    .insert(jdbi)
                    .toEntity();
            payoutEntityList.add(entity);
        }

        return payoutEntityList;
    }

    public static final class PayoutFixtureBuilder {
        private Long id = RandomUtils.nextLong(1, 99999);
        private String gatewayPayoutId = RandomStringUtils.randomAlphanumeric(10);
        private Long amount = 1000L;
        private ZonedDateTime createdDate = now(UTC).minusDays(1L);
        private ZonedDateTime paidOutDate = now(UTC);
        private PayoutState state = PayoutState.PAID_OUT;
        private Integer eventCount = 1;
        private String payoutDetails = "{}";
        private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);

        private PayoutFixtureBuilder() {
        }

        public static PayoutFixtureBuilder aPayoutFixture() {
            return new PayoutFixtureBuilder();
        }

        public PayoutFixtureBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public PayoutFixtureBuilder withGatewayPayoutId(String gatewayPayoutId) {
            this.gatewayPayoutId = gatewayPayoutId;
            return this;
        }

        public PayoutFixtureBuilder withAmount(Long amount) {
            this.amount = amount;
            return this;
        }

        public PayoutFixtureBuilder withCreatedDate(ZonedDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public PayoutFixtureBuilder withPaidOutDate(ZonedDateTime paidOutDate) {
            this.paidOutDate = paidOutDate;
            return this;
        }

        public PayoutFixtureBuilder withState(PayoutState state) {
            this.state = state;
            return this;
        }

        public PayoutFixtureBuilder withEventCount(Integer eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public PayoutFixtureBuilder withPayoutDetails(String payoutDetails) {
            this.payoutDetails = payoutDetails;
            return this;
        }

        public PayoutFixtureBuilder withGatewayAccountId(String gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }
        public PayoutFixture build() {
            PayoutFixture payoutFixture = new PayoutFixture();
            payoutFixture.amount = this.amount;
            payoutFixture.gatewayPayoutId = this.gatewayPayoutId;
            payoutFixture.id = this.id;
            payoutFixture.state = this.state;
            payoutFixture.createdDate = this.createdDate;
            payoutFixture.paidOutDate = this.paidOutDate;
            payoutFixture.eventCount = this.eventCount;
            payoutFixture.payoutDetails = this.payoutDetails;
            payoutFixture.gatewayAccountId = this.gatewayAccountId;
            return payoutFixture;
        }
    }
}
