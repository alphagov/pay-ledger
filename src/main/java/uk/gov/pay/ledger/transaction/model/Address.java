package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(addressLine1, address.addressLine1) &&
                Objects.equals(addressLine2, address.addressLine2) &&
                Objects.equals(addressPostCode, address.addressPostCode) &&
                Objects.equals(addressCity, address.addressCity) &&
                Objects.equals(addressCounty, address.addressCounty) &&
                Objects.equals(addressCountry, address.addressCountry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, addressPostCode, addressCity, addressCounty, addressCountry);
    }
}
