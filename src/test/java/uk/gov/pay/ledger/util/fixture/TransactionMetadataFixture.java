package uk.gov.pay.ledger.util.fixture;

import org.jdbi.v3.core.Jdbi;

public class TransactionMetadataFixture {

    private long transactionId;
    private String metadataKey;
    private String value;

    public static TransactionMetadataFixture aTransactionMetadataFixture() {
        return new TransactionMetadataFixture();
    }

    public TransactionMetadataFixture insert(Jdbi jdbi) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO" +
                                "    transaction_metadata(" +
                                "        transaction_id," +
                                "        metadata_key_id," +
                                "        value" +
                                "    )" +
                                "VALUES(?, (select id from metadata_key where key = ?), ?)",
                        transactionId,
                        metadataKey,
                        value
                )
        );
        return this;
    }

    public TransactionMetadataFixture withTransactionId(long transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public TransactionMetadataFixture withMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
        return this;
    }

    public TransactionMetadataFixture withValue(String value) {
        this.value = value;
        return this;
    }
}
