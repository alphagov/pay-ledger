package uk.gov.pay.ledger.util.fixture;

import org.jdbi.v3.core.Jdbi;

public class GatewayAccountMetadataFixture {

    private String gatewayAccountId;
    private String metadataKey;
    private String value;

    public static GatewayAccountMetadataFixture aGatewayAccountMetadataFixture() {
        return new GatewayAccountMetadataFixture();
    }

    public GatewayAccountMetadataFixture insert(Jdbi jdbi) {
        jdbi.withHandle(h ->
                h.execute(
                        "INSERT INTO" +
                                "    gateway_account_metadata(" +
                                "        gateway_account_id," +
                                "        metadata_key_id" +
                                "    )" +
                                "VALUES(?, (select id from metadata_key where key = ?))",
                        gatewayAccountId,
                        metadataKey
                )
        );
        return this;
    }

    public GatewayAccountMetadataFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public GatewayAccountMetadataFixture withMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
        return this;
    }
}
