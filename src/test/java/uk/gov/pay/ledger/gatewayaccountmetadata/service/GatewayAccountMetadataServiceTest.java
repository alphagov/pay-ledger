package uk.gov.pay.ledger.gatewayaccountmetadata.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.gatewayaccountmetadata.dao.GatewayAccountMetadataDao;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayAccountMetadataServiceTest {

    @InjectMocks
    GatewayAccountMetadataService gatewayAccountMetadataService;

    @Mock
    GatewayAccountMetadataDao gatewayAccountMetadataDao;

    @Test
    void shouldInsertMetadata() {
        gatewayAccountMetadataService.upsertMetadataKeyForGatewayAccount("accnt-id", "meta-key-1");

        verify(gatewayAccountMetadataDao).upsert("accnt-id", "meta-key-1");
    }
}