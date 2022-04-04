package uk.gov.pay.ledger.transaction.search.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RefundSummary {

    @Schema(example = "unavailable")
    private String status;

    private String userExternalId;

    private Long amountAvailable;

    private Long amountSubmitted;

    private Long amountRefunded;

    public RefundSummary() {
    }

    public RefundSummary(String status, Long amountAvailable, Long amountRefunded) {
        this.status = status;
        this.amountAvailable = amountAvailable;
        this.amountRefunded = amountRefunded;
        this.amountSubmitted = amountRefunded;
    }

    public static RefundSummary from(TransactionEntity entity) {
        if (entity.getRefundStatus() == null &&
                entity.getRefundAmountAvailable() == null &&
                entity.getRefundAmountRefunded() == null) {
            return null;
        }

        return new RefundSummary(entity.getRefundStatus(), entity.getRefundAmountAvailable(), entity.getRefundAmountRefunded());
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public Long getAmountAvailable() {
        return amountAvailable;
    }

    public Long getAmountRefunded() {
        return amountRefunded;
    }

    public Long getAmountSubmitted() {
        return amountSubmitted;
    }

    public String getStatus() {
        return status;
    }

    public static RefundSummary ofValue(String status, Long amountAvailable, Long amountRefunded) {
        return new RefundSummary(status, amountAvailable, amountRefunded);
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
        return Objects.equals(amountRefunded, that.amountRefunded);
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (userExternalId != null ? userExternalId.hashCode() : 0);
        result = 31 * result + (amountAvailable != null ? amountAvailable.hashCode() : 0);
        result = 31 * result + (amountRefunded != null ? amountRefunded.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefundSummary{" +
                "status='" + status + '\'' +
                "userExternalId='" + userExternalId + '\'' +
                ", amountAvailable=" + amountAvailable +
                ", amountRefunded=" + amountRefunded +
                '}';
    }
}
