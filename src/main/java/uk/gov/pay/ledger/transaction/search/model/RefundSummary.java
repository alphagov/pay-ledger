package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RefundSummary {

    private String status;

    private String userExternalId;

    private Long amountAvailable;

    private Long amountSubmitted;

    public RefundSummary(String status, Long amountAvailable, Long amountSubmitted) {
        this.status = status;
        this.amountAvailable = amountAvailable;
        this.amountSubmitted = amountSubmitted;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAmountAvailable(Long amountAvailable) {
        this.amountAvailable = amountAvailable;
    }

    public void setAmountSubmitted(Long amountSubmitted) {
        this.amountSubmitted = amountSubmitted;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public void setUserExternalId(String userExternalId) {
        this.userExternalId = userExternalId;
    }

    public Long getAmountAvailable() {
        return amountAvailable;
    }

    public Long getAmountSubmitted() {
        return amountSubmitted;
    }

    public String getStatus() {
        return status;
    }

    public static RefundSummary ofValue(String status, Long amountAvailable, Long amountSubmitted) {
        return new RefundSummary(status, amountAvailable, amountSubmitted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RefundSummary that = (RefundSummary) o;

        if (!status.equals(that.status)) {
            return false;
        }
        if (!Objects.equals(userExternalId, that.userExternalId)) {
            return false;
        }
        if (!Objects.equals(amountAvailable, that.amountAvailable)) {
            return false;
        }
        return Objects.equals(amountSubmitted, that.amountSubmitted);
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (userExternalId != null ? userExternalId.hashCode() : 0);
        result = 31 * result + (amountAvailable != null ? amountAvailable.hashCode() : 0);
        result = 31 * result + (amountSubmitted != null ? amountSubmitted.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefundSummary{" +
                "status='" + status + '\'' +
                "userExternalId='" + userExternalId + '\'' +
                ", amountAvailable=" + amountAvailable +
                ", amountSubmitted=" + amountSubmitted +
                '}';
    }
}
