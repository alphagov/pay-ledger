package uk.gov.pay.ledger.util.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.LocalDate;

import static java.time.LocalDate.parse;


public class TransactionSummaryFixture {
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(10);
    private TransactionType type = TransactionType.PAYMENT;
    private TransactionState state = TransactionState.SUCCESS;
    private LocalDate transactionDate = parse("2018-09-22");
    private boolean live;
    private boolean moto;
    private Long amount = RandomUtils.nextLong(1, 99999);
    private Long fee = 0L;
    private Long noOfTransactions = 10L;

    public static TransactionSummaryFixture aTransactionSummaryFixture() {
        return new TransactionSummaryFixture();
    }

    public TransactionSummaryFixture insert(Jdbi jdbi) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO transaction_summary (" +
                                "        gateway_account_id," +
                                "        type," +
                                "        transaction_date," +
                                "        state," +
                                "        live," +
                                "        moto," +
                                "        total_amount_in_pence," +
                                "        no_of_transactions," +
                                "        total_fee_in_pence" +
                                "    )" +
                                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        gatewayAccountId,
                        type.name(),
                        transactionDate,
                        state.name(),
                        live,
                        moto,
                        amount,
                        noOfTransactions,
                        fee
                )
        );
        return this;
    }

    public TransactionSummaryFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public TransactionSummaryFixture withType(TransactionType type) {
        this.type = type;
        return this;
    }

    public TransactionSummaryFixture withState(TransactionState state) {
        this.state = state;
        return this;
    }

    public TransactionSummaryFixture withTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
        return this;
    }

    public TransactionSummaryFixture withLive(boolean live) {
        this.live = live;
        return this;
    }

    public TransactionSummaryFixture withMoto(boolean moto) {
        this.moto = moto;
        return this;
    }

    public TransactionSummaryFixture withAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public TransactionSummaryFixture withFee(Long fee) {
        this.fee = fee;
        return this;
    }

    public TransactionSummaryFixture withNoOfTransactions(Long noOfTransactions) {
        this.noOfTransactions = noOfTransactions;
        return this;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionState getState() {
        return state;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public boolean isLive() {
        return live;
    }

    public boolean isMoto() {
        return moto;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getFee() {
        return fee;
    }

    public Long getNoOfTransactions() {
        return noOfTransactions;
    }
}
