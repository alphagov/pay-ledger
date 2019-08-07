package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TransactionsForTransactionResponse {

    private final String parentTransactionId;
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
