package uk.gov.pay.ledger.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.ResourceType;
import uk.gov.pay.ledger.event.model.TransactionEntityFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.model.TransactionEvent;
import uk.gov.pay.ledger.transaction.model.TransactionEventResponse;
import uk.gov.pay.ledger.transaction.model.TransactionFactory;
import uk.gov.pay.ledger.transaction.model.TransactionSearchResponse;
import uk.gov.pay.ledger.transaction.search.common.CommaDelimitedSetParameter;
import uk.gov.pay.ledger.transaction.search.common.TransactionSearchParams;
import uk.gov.pay.ledger.transaction.search.model.PaginationBuilder;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {

    @Mock
    private TransactionDao mockTransactionDao;
    @Mock
    private EventDao mockEventDao;
    @Mock
    private UriInfo mockUriInfo;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TransactionService transactionService;
    private String gatewayAccountId = "gateway_account_id";
    private TransactionSearchParams searchParams;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        TransactionEntityFactory transactionEntityFactory = new TransactionEntityFactory(objectMapper);
        TransactionFactory transactionFactory = new TransactionFactory(objectMapper);
        transactionService = new TransactionService(mockTransactionDao, mockEventDao, transactionEntityFactory, transactionFactory);
        searchParams = new TransactionSearchParams();
        searchParams.setAccountId(gatewayAccountId);

        when(mockUriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://app.com"));
        when(mockUriInfo.getPath()).thenReturn("/v1/transaction");
    }

    @Test
    public void shouldReturnAListOfTransactions() {
        List<TransactionEntity> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 5);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(5L);
        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        assertThat(transactionSearchResponse.getGatewayExternalId(), is(gatewayAccountId));
        assertThat(transactionSearchResponse.getPage(), is(1L));
        assertThat(transactionSearchResponse.getCount(), is(5L));
        assertThat(transactionSearchResponse.getTotal(), is(5L));
        assertThat(transactionSearchResponse.getTransactionViewList().size(), is(5));
    }

    @Test
    public void shouldListTransactionsWithAllPaginationLinks() {
        List<TransactionEntity> transactionViewList = TransactionFixture
                .aTransactionList(gatewayAccountId, 100);
        searchParams.setPageNumber(3l);
        searchParams.setDisplaySize(10l);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(100L);

        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        PaginationBuilder paginationBuilder = transactionSearchResponse.getPaginationBuilder();

        assertThat(paginationBuilder.getFirstLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=1&display_size=10"));
        assertThat(paginationBuilder.getPrevLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=2&display_size=10"));
        assertThat(paginationBuilder.getSelfLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=3&display_size=10"));
        assertThat(paginationBuilder.getNextLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=4&display_size=10"));
        assertThat(paginationBuilder.getLastLink().getHref(), is("http://app.com/v1/transaction?account_id=gateway_account_id&page=10&display_size=10"));
    }

    @Test
    public void shouldListTransactionsWithCorrectQueryParamsForPaginationLinks() {
        List<TransactionEntity> transactionViewList = TransactionFixture.aTransactionList(gatewayAccountId, 10);
        when(mockTransactionDao.searchTransactions(any(TransactionSearchParams.class))).thenReturn(transactionViewList);
        when(mockTransactionDao.getTotalForSearch(any(TransactionSearchParams.class))).thenReturn(10L);

        searchParams.setEmail("test@email.com");
        searchParams.setCardHolderName("test");
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-06-01T10:15:30Z");
        searchParams.setReference("ref");
        searchParams.setFirstDigitsCardNumber("4242");
        searchParams.setLastDigitsCardNumber("1234");
        searchParams.setPaymentStates(new CommaDelimitedSetParameter("created,submitted"));
        searchParams.setRefundStates(new CommaDelimitedSetParameter("created,refunded"));
        searchParams.setCardBrands(new CommaDelimitedSetParameter("visa,mastercard"));

        TransactionSearchResponse transactionSearchResponse = transactionService.searchTransactions(searchParams, mockUriInfo);
        PaginationBuilder paginationBuilder = transactionSearchResponse.getPaginationBuilder();
        String selfLink = paginationBuilder.getSelfLink().getHref();

        assertThat(selfLink, containsString("email=test%40email.com"));
        assertThat(selfLink, containsString("reference=ref"));
        assertThat(selfLink, containsString("cardholder_name=test"));
        assertThat(selfLink, containsString("first_digits_card_number=4242"));
        assertThat(selfLink, containsString("last_digits_card_number=1234"));
        assertThat(selfLink, containsString("payment_states=created%2Csubmitted"));
        assertThat(selfLink, containsString("refund_states=created%2Crefunded"));
        assertThat(selfLink, containsString("card_brand=visa%2Cmastercard"));
    }

    @Test
    public void findTransactionEvents_shouldReturnTransactionEventsCorrectly() {
        List<TransactionEntity> transactionEntityList = TransactionFixture.aTransactionList(gatewayAccountId, 1);
        when(mockTransactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(anyString(), anyString()))
                .thenReturn(transactionEntityList);

        Event event = EventFixture.anEventFixture().withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        List<Event> eventList = List.of(event);
        when(mockEventDao.findEventsForExternalIds(any())).thenReturn(eventList);

        TransactionEventResponse transactionEventResponse
                = transactionService.findTransactionEvents("external-id", gatewayAccountId, false);

        List<TransactionEvent> transactionEvents = transactionEventResponse.getEvents();

        assertThat(transactionEventResponse.getTransactionId(), is("external-id"));
        assertThat(transactionEvents.size(), is(1));
        assertTransactionEvent(event, transactionEvents.get(0), transactionEntityList.get(0).getAmount(), "created");
    }

    @Test
    public void findTransactionEvents_shouldFilterEventWithoutAMappingToTransactionState() {
        List<TransactionEntity> transactionEntityList = TransactionFixture.aTransactionList(gatewayAccountId, 2);
        when(mockTransactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(anyString(), anyString()))
                .thenReturn(transactionEntityList);

        Event event = EventFixture.anEventFixture().withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        Event eventWithoutStateMapping = EventFixture.anEventFixture()
                .withEventType("EVENT_WITHOUT_ANY_STATE_MAPPING")
                .withResourceExternalId(transactionEntityList.get(1).getExternalId()).toEntity();
        List<Event> eventList = List.of(event, eventWithoutStateMapping);

        when(mockEventDao.findEventsForExternalIds(any())).thenReturn(eventList);

        TransactionEventResponse transactionEventResponse
                = transactionService.findTransactionEvents("external-id", gatewayAccountId, false);

        List<TransactionEvent> transactionEvents = transactionEventResponse.getEvents();

        assertThat(transactionEventResponse.getTransactionId(), is("external-id"));
        assertThat(transactionEvents.size(), is(1));

        assertTransactionEvent(event, transactionEvents.get(0), transactionEntityList.get(0).getAmount(), "created");
    }

    @Test
    public void findTransactionEvents_shouldThrowBadRequestExceptionIfNotRecordsFound() {
        when(mockTransactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(anyString(), anyString()))
                .thenReturn(new ArrayList<>());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Transaction with id [external-id] not found");

        TransactionEventResponse transactionEventResponse
                = transactionService.findTransactionEvents("external-id", gatewayAccountId, false);
    }

    @Test
    public void findTransactionEvents_shouldRemoveDuplicateEventsByExternalIdResourceTypeAndState() {
        List<TransactionEntity> transactionEntityList = TransactionFixture.aTransactionList(gatewayAccountId, 1);
        when(mockTransactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(anyString(), anyString()))
                .thenReturn(transactionEntityList);

        Event event1ForStateSubmitted = EventFixture.anEventFixture()
                .withEventType("AUTHORISATION_SUCCESSFUL")
                .withEventDate(ZonedDateTime.now())
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        Event event2ForStateSubmitted = EventFixture.anEventFixture()
                .withEventType("USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL")
                .withEventDate(ZonedDateTime.now().plusDays(1))
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        Event event1ForStateError = EventFixture.anEventFixture()
                .withEventType("REFUND_SUBMITTED")
                .withResourceType(ResourceType.REFUND)
                .withEventDate(ZonedDateTime.now().plusDays(2))
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();

        List<Event> eventList = List.of(event1ForStateSubmitted, event2ForStateSubmitted, event1ForStateError);
        when(mockEventDao.findEventsForExternalIds(any())).thenReturn(eventList);

        TransactionEventResponse transactionEventResponse
                = transactionService.findTransactionEvents("external-id", gatewayAccountId, false);

        List<TransactionEvent> transactionEvents = transactionEventResponse.getEvents();

        assertThat(transactionEventResponse.getTransactionId(), is("external-id"));
        assertThat(transactionEvents.size(), is(2));

        assertTransactionEvent(event1ForStateSubmitted, transactionEvents.get(0), transactionEntityList.get(0).getAmount(), "submitted");
        assertTransactionEvent(event1ForStateError, transactionEvents.get(1), transactionEntityList.get(0).getAmount(), "submitted");
    }

    @Test
    public void findTransactionEvents_shouldReturnAllEventsIfIncludeAllEventsIsTrue() {
        List<TransactionEntity> transactionEntityList = TransactionFixture.aTransactionList(gatewayAccountId, 1);
        when(mockTransactionDao.findTransactionByExternalOrParentIdAndGatewayAccountId(anyString(), anyString()))
                .thenReturn(transactionEntityList);

        Event event1ForStateSubmitted = EventFixture.anEventFixture()
                .withEventType("AUTHORISATION_SUCCESSFUL")
                .withEventDate(ZonedDateTime.now())
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        Event event2ForStateSubmitted = EventFixture.anEventFixture()
                .withEventType("USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL")
                .withEventDate(ZonedDateTime.now().plusDays(1))
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();
        Event eventForUnknownType = EventFixture.anEventFixture()
                .withEventType("UNKNOWN_EVENT")
                .withEventDate(ZonedDateTime.now().plusDays(1))
                .withResourceExternalId(transactionEntityList.get(0).getExternalId()).toEntity();

        List<Event> eventList = List.of(event1ForStateSubmitted, event2ForStateSubmitted, eventForUnknownType);
        when(mockEventDao.findEventsForExternalIds(any())).thenReturn(eventList);

        TransactionEventResponse transactionEventResponse
                = transactionService.findTransactionEvents("external-id", gatewayAccountId, true);

        List<TransactionEvent> transactionEvents = transactionEventResponse.getEvents();

        assertThat(transactionEventResponse.getTransactionId(), is("external-id"));
        assertThat(transactionEvents.size(), is(3));

        assertTransactionEvent(event1ForStateSubmitted, transactionEvents.get(0), transactionEntityList.get(0).getAmount(), "submitted");
        assertTransactionEvent(event2ForStateSubmitted, transactionEvents.get(1), transactionEntityList.get(0).getAmount(), "submitted");
        assertTransactionEvent(eventForUnknownType, transactionEvents.get(2), transactionEntityList.get(0).getAmount(), null);
    }

    private void assertTransactionEvent(Event event, TransactionEvent transactionEvent, Long amount, String state) {
        assertThat(transactionEvent.getState() == null ? null : transactionEvent.getState().getState(), is(state));
        assertThat(transactionEvent.getAmount(), is(amount));
        assertThat(transactionEvent.getData(), is(event.getEventData()));
        assertThat(transactionEvent.getEventType(), is(event.getEventType()));
        assertThat(transactionEvent.getResourceType(), is(event.getResourceType().toString().toUpperCase()));
        assertThat(transactionEvent.getTimestamp(), is(event.getEventDate()));
    }
}