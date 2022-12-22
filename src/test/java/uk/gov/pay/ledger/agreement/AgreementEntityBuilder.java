package uk.gov.pay.ledger.agreement;

import uk.gov.pay.ledger.agreement.entity.AgreementEntity;

public class AgreementEntityBuilder {

    private String agreementId;
    private int eventCount;
    private String accountId = null;
    private String serviceId = null;

    private AgreementEntityBuilder(String agreementId) {
        this.agreementId = agreementId;
        eventCount = 1;
    }

    public static AgreementEntityBuilder anAgreementEntityWithId(String agreementId) {
        return new AgreementEntityBuilder(agreementId);
    }

    public AgreementEntityBuilder withEventCount(int eventCount) {
        this.eventCount = eventCount;
        return this;
    }

    public AgreementEntityBuilder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public AgreementEntityBuilder withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public AgreementEntity build() {
        var agreementEntity = new AgreementEntity();
        agreementEntity.setExternalId(agreementId);
        agreementEntity.setEventCount(eventCount);
        if (accountId != null) {
            agreementEntity.setGatewayAccountId(accountId);
        }
        if (serviceId != null) {
            agreementEntity.setServiceId(serviceId);
        }
        return agreementEntity;
    }
}
