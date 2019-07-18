package uk.gov.pay.ledger.transaction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEntity {

    @JsonIgnore
    private Long id;
    @JsonProperty("gateway_account_id")
    private String gatewayAccountId;

    @Setter
    @JsonIgnore
    private String externalId;
    private Long amount;
    private String reference;
    private String description;

    @Setter
    private String state;
    private String email;
    @JsonProperty("cardholder_name")
    private String cardholderName;
    @JsonProperty("external_metadata")
    private String externalMetadata;

    @Setter
    @JsonIgnore
    private ZonedDateTime createdDate;

    @Setter
    @JsonIgnore
    private String transactionDetails;

    @Setter
    @JsonIgnore
    private Integer eventCount;
    @JsonProperty("card_brand")
    private String cardBrand;
    @JsonProperty("last_digits_card_number")
    private String lastDigitsCardNumber;
    @JsonProperty("first_digits_card_number")
    private String firstDigitsCardNumber;


    private Long netAmount;
    private Long totalAmount;
    private ZonedDateTime settlementSubmittedTime;
    private ZonedDateTime settledTime;
    private String refundStatus;
    private Long refundAmountSubmitted;
    private Long refundAmountAvailable;
}
