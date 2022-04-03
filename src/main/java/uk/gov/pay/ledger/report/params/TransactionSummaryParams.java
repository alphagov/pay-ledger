package uk.gov.pay.ledger.report.params;

import io.swagger.v3.oas.annotations.Parameter;
import uk.gov.service.payments.commons.validation.ValidDate;

import javax.ws.rs.QueryParam;
import java.util.Objects;

public class TransactionSummaryParams {

    @QueryParam("account_id")
    @Parameter(example = "1", description = "Gateway account ID. Required when override_account_id_restriction is 'false'")
    private String accountId;

    @QueryParam("include_moto_statistics")
    @Parameter(example = "false", description = "Set to true to return statistics for moto transactions")
    private boolean includeMotoStatistics;

    @QueryParam("from_date")
    @Parameter(description = "From date of transactions to be searched (this date is exclusive).", example = "2022-03-29T00:00:00Z")
    @ValidDate(message = "Invalid attribute value: from_date. Must be a valid date")
    private String fromDate;

    @QueryParam("to_date")
    @Parameter(description = "To date of transactions to be searched (this date is exclusive).", example = "2022-03-29T00:00:00Z")
    @ValidDate(message = "Invalid attribute value: to_date. Must be a valid date")
    private String toDate;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isIncludeMotoStatistics() {
        return includeMotoStatistics;
    }

    public void setIncludeMotoStatistics(boolean includeMotoStatistics) {
        this.includeMotoStatistics = includeMotoStatistics;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSummaryParams that = (TransactionSummaryParams) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(includeMotoStatistics, that.includeMotoStatistics) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, includeMotoStatistics, fromDate, toDate);
    }

    @Override
    public String toString() {
        return "TransactionSummaryParams{" +
                "accountId='" + accountId + '\'' +
                "moto='" + includeMotoStatistics + '\'' +
                ", fromDate='" + fromDate + '\'' +
                ", toDate='" + toDate + '\'' +
                '}';
    }
}
