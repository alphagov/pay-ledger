package uk.gov.pay.ledger.agreement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentInstrumentEntity {
    private String externalId;
    private String agreementExternalId;
    private String email;
    private String cardholderName;
    private String addressLine1;
    private String addressLine2;
    private String addressPostcode;
    private String addressCity;

    private String addressCounty;
    private String addressCountry;

    private String lastDigitsCardNumber;
    private String expiryDate;
    private String cardBrand;

    private ZonedDateTime createdDate;
    private Integer eventCount;

    public PaymentInstrumentEntity() {

    }

    public PaymentInstrumentEntity(String externalId, String agreementExternalId, String email, String cardholderName, String addressLine1, String addressLine2, String addressPostcode, String addressCity, String addressCounty, String addressCountry, String lastDigitsCardNumber, String expiryDate, String cardBrand, ZonedDateTime createdDate, Integer eventCount) {
        this.externalId = externalId;
        this.agreementExternalId = agreementExternalId;
        this.email = email;
        this.cardholderName = cardholderName;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressPostcode = addressPostcode;
        this.addressCity = addressCity;
        this.addressCounty = addressCounty;
        this.addressCountry = addressCountry;
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.expiryDate = expiryDate;
        this.cardBrand = cardBrand;
        this.createdDate = createdDate;
        this.eventCount = eventCount;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setAgreementExternalId(String agreementExternalId) {
        this.agreementExternalId = agreementExternalId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public void setLastDigitsCardNumber(String lastDigitsCardNumber) {
        this.lastDigitsCardNumber = lastDigitsCardNumber;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getAgreementExternalId() {
        return agreementExternalId;
    }

    public String getEmail() {
        return email;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getEventCount() {
        return eventCount;
    }
    public String getAddressCounty() {
        return addressCounty;
    }
}