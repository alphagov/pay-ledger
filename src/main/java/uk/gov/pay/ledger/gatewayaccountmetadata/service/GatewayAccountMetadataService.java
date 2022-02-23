package uk.gov.pay.ledger.gatewayaccountmetadata.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.gatewayaccountmetadata.dao.GatewayAccountMetadataDao;

import java.util.List;


public class GatewayAccountMetadataService {

    private final GatewayAccountMetadataDao gatewayAccountMetadataDao;

    @Inject
    public GatewayAccountMetadataService(GatewayAccountMetadataDao gatewayAccountMetadataDao) {
        this.gatewayAccountMetadataDao = gatewayAccountMetadataDao;
    }

    public void upsertMetadataKeyForGatewayAccount(String gatewayAcctId, String metadataKey) {
        gatewayAccountMetadataDao.upsert(gatewayAcctId, metadataKey);
    }

    public List<String> getKeysForGatewayAccounts(List<String> gatewayAcctIds) {
        return gatewayAccountMetadataDao.findMetadataKeysForGatewayAccounts(gatewayAcctIds);
    }

}
