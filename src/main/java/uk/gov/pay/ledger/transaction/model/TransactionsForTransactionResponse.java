package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionsForTransactionResponse {

    @Schema(example = "9np5pocnotgkpp029d5kdfau5f")
    private final String parentTransactionId;
    @ArraySchema(schema = @Schema(implementation = TransactionView.class))
    private final List<TransactionView> transactions;

    private TransactionsForTransactionResponse(String parentTransactionId, List<TransactionView> transactions) {
        this.parentTransactionId = parentTransactionId;
        this.transactions = transactions;
    }

    public static TransactionsForTransactionResponse of(String parentTransactionId, List<TransactionView> transactions) {
        return new TransactionsForTransactionResponse(parentTransactionId, transactions);
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public List<TransactionView> getTransactions() {
        return transactions;
    }
}
