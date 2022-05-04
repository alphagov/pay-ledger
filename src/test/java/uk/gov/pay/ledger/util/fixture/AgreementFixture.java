package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.agreement.entity.AgreementEntity;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AgreementFixture implements DbFixture<AgreementFixture, AgreementEntity> {

    private Long id = RandomUtils.nextLong(1, 99999);
    private String serviceId = RandomStringUtils.randomAlphanumeric(26);
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
    private String externalId = RandomStringUtils.randomAlphanumeric(20);
    private String reference = RandomStringUtils.randomAlphanumeric(10);
    private String description = RandomStringUtils.randomAlphanumeric(20);
    private String status = "ACTIVE";
    private boolean live = false;
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC);
    private Integer eventCount = 1;

    private AgreementFixture() {
    }

    public static AgreementFixture anAgreementFixture(String externalId, String serviceId) {
        var fixture = new AgreementFixture();
        fixture.setExternalId(externalId);
        fixture.setServiceId(serviceId);
        return fixture;
    }

    public static AgreementFixture anAgreementFixture() {
        return new AgreementFixture();
    }

    @Override
    public AgreementFixture insert(Jdbi jdbi) {
        var sql = "INSERT INTO agreement" +
                "(id, external_id, gateway_account_id, service_id, reference, description, status, live, created_date, event_count) " +
                "VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                        eventCount
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
                null
        );
    }

    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getDescription() {
        return description;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }
}