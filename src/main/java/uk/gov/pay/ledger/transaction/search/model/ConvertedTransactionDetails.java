package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvertedTransactionDetails {

    public ConvertedTransactionDetails() {}

    @JsonProperty("language")
    private String language;
    @JsonProperty("payment_provider")
    private String paymentProvider;
    @JsonProperty("expiry_date")
    private String expiryDate;
    @JsonProperty("address_line1")
    private String addressLine1;
    @JsonProperty("address_line2")
    private String addressLine2;
    @JsonProperty("address_postcode")
    private String addressPostcode;
    @JsonProperty("address_city")
    private String addressCity;
    @JsonProperty("address_county")
    private String addressCounty;
    @JsonProperty("address_country")
    private String addressCountry;
    @JsonProperty("wallet")
    private String wallet;
    @JsonProperty("delayed_capture")
    private Boolean delayedCapture;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonProperty("gateway_transaction_id")
    private String gatewayTransactionId;
}
