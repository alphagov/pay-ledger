package uk.gov.pay.ledger.transaction.model;

import uk.gov.pay.ledger.event.model.EventDigest;

import java.util.HashMap;
import java.util.Map;

public class MetadataKey {
    private String externalId;
    private Map<String, Object> metadata;

    public MetadataKey(String externalId, Map<String, Object> metadata) {
        this.externalId = externalId;
        this.metadata = metadata;
    }

    public String getExternalId() {
        return externalId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static MetadataKey from(EventDigest eventDigest) {
        Map<String, Object> metadata = (Map<String, Object>) eventDigest.getEventPayload().get("external_metadata");

        return new MetadataKey(eventDigest.getResourceExternalId(),
                metadata == null ? new HashMap<>() : metadata);
    }
}
