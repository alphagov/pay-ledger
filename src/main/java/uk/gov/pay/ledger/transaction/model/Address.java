package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    @Schema(example = "address line 1")
    private String addressLine1;
    @Schema(example = "address line 2")
    private String addressLine2;
    @Schema(example = "Ex 8RR")
    private String addressPostCode;
    @Schema(example = "London")
    private String addressCity;
    @Schema(example = "county")
    private String addressCounty;
    @Schema(example = "GB")
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

    public static Address from(String addressLine1, String addressLine2, String addressPostcode, String addressCity, String addressCounty, String addressCountry) {
        if (addressLine1 == null &&
                addressLine2 == null &&
                addressPostcode == null &&
                addressCity == null &&
                addressCounty == null &&
                addressCountry == null) {
            return null;
        }

        return new Address(addressLine1, addressLine2, addressPostcode, addressCity, addressCounty, addressCountry);
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
