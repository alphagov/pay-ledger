package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {

    private String addressLine1;
    private String addressLine2;
    private String addressPostCode;
    private String addressCity;
    private String addressCounty;
    private String addressCountry;

    public Address(String addressLine1, String addressLine2, String addressPostCode,
                   String addressCity, String addressCounty, String addressCountry) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressPostCode = addressPostCode;
        this.addressCity = addressCity;
        this.addressCounty = addressCounty;
        this.addressCountry = addressCountry;
    }

    @JsonProperty("line1")
    public String getAddressLine1() {
        return addressLine1;
    }

    @JsonProperty("line2")
    public String getAddressLine2() {
        return addressLine2;
    }

    @JsonProperty("postcode")
    public String getAddressPostCode() {
        return addressPostCode;
    }

    @JsonProperty("city")
    public String getAddressCity() {
        return addressCity;
    }

    @JsonProperty("county")
    public String getAddressCounty() {
        return addressCounty;
    }

    @JsonProperty("country")
    public String getAddressCountry() {
        return addressCountry;
    }
}
