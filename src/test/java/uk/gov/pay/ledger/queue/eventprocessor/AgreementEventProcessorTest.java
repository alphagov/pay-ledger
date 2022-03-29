package uk.gov.pay.ledger.queue.eventprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.ledger.agreement.service.AgreementService;
import uk.gov.pay.ledger.event.service.EventService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AgreementEventProcessorTest {
    @Mock
    private EventService eventService;
    @Mock
    private AgreementService agreementService;

    private AgreementEventProcessor agreementEventProcessor;

    @BeforeEach
    void setUp() {
        agreementEventProcessor = new AgreementEventProcessor(eventService, agreementService);
    }
}