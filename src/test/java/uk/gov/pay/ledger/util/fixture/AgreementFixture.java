package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;
import uk.gov.pay.ledger.agreement.entity.PaymentInstrumentEntity;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AgreementFixture implements DbFixture<AgreementFixture, AgreementEntity> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String serviceId = "a-service-id";
    private String gatewayAccountId = "1";
    private String externalId = "an-external-id";
    private String reference = "a-reference";
    private String description = "a description";
    private AgreementStatus status = AgreementStatus.ACTIVE;
    private boolean live = false;
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC);
    private Integer eventCount = 1;
    private String userIdentifier;

    private AgreementFixture() {
    }

    public static AgreementFixture anAgreementFixture() {
        return new AgreementFixture();
    }

    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public AgreementFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public AgreementFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public AgreementFixture withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public AgreementFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public AgreementFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public AgreementFixture withStatus(AgreementStatus status) {
        this.status = status;
        return this;
    }

    public AgreementFixture withLive(Boolean live) {
        this.live = live;
        return this;
    }

    public AgreementFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public AgreementFixture withEventCount(Integer eventCount) {
        this.eventCount = eventCount;
        return this;
    }

    public AgreementFixture withUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
        return this;
    }

    @Override
    public AgreementFixture insert(Jdbi jdbi) {
        var sql = "INSERT INTO agreement" +
                "(id, external_id, gateway_account_id, service_id, reference, description, status, live, created_date, event_count, user_identifier) " +
                "VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbi.withHandle(h ->
                h.execute(
                        sql,
                        id,
                        externalId,
                        gatewayAccountId,
                        serviceId,
                        reference,
                        description,
                        status,
                        live,
                        createdDate,
                        eventCount,
                        userIdentifier
                )
        );
        return this;
    }

    @Override
    public AgreementEntity toEntity() {
        return new AgreementEntity(
                externalId,
                gatewayAccountId,
                serviceId,
                reference,
                description,
                status,
                live,
                createdDate,
                eventCount,
                null,
                userIdentifier);
    }
}