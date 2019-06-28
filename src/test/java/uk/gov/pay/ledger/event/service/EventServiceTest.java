package uk.gov.pay.ledger.event.service;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.event.model.SalientEventType;
import uk.gov.pay.ledger.event.model.response.CreateEventResponse;
import uk.gov.pay.ledger.util.fixture.EventFixture;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {
    private final long amount = 2000L;
    private final String description = "a payment";
    private final String reference = "my reference";
    private final String returnUrl = "https://example.com";
    private final String gatewayAccountId = "gateway_account_id";
    private final String paymentProvider = "stripe";
    private final String email = "bob@example.com";
    private final String cardholderName = "Bob";
    private final String addressLine1 = "13 Pudding Lane";
    private final String addressLine2 = "Clerkenwell";
    private final String postcode = "EC1 1UT";
    private final String city = "London";
    private final String county = "London";
    private final String country = "UK";

    @Mock
    EventDao mockEventDao;

    private EventService eventService;

    private Event event;

    private ZonedDateTime latestEventTime;
    private final String resourceExternalId = "resource_external_id";
    private Event paymentCreatedEvent;
    private Event paymentDetailsEvent;
    private EventDigest eventDigest;

    @Before
    public void setUp() {
        eventService = new EventService(mockEventDao);

        latestEventTime = ZonedDateTime.now().minusHours(1L);
        JsonObject paymentCreatedEventDetails = new JsonObject();
        paymentCreatedEventDetails.addProperty("amount", amount);
        paymentCreatedEventDetails.addProperty("description", description);
        paymentCreatedEventDetails.addProperty("reference", reference);
        paymentCreatedEventDetails.addProperty("return_url", returnUrl);
        paymentCreatedEventDetails.addProperty("gateway_account_id", gatewayAccountId);
        paymentCreatedEventDetails.addProperty("payment_provider", paymentProvider);
        paymentCreatedEvent = EventFixture.anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventData(paymentCreatedEventDetails.toString())
                .toEntity();

        JsonObject paymentDetailsEventDetails = new JsonObject();
        paymentDetailsEventDetails.addProperty("email", email);
        paymentDetailsEventDetails.addProperty("cardholder_name", cardholderName);
        paymentDetailsEventDetails.addProperty("address_line1", addressLine1);
        paymentDetailsEventDetails.addProperty("address_line2", addressLine2);
        paymentDetailsEventDetails.addProperty("address_postcode", postcode);
        paymentDetailsEventDetails.addProperty("address_city", city);
        paymentDetailsEventDetails.addProperty("address_county", county);
        paymentDetailsEventDetails.addProperty("address_country", country);
        paymentDetailsEvent = EventFixture.anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventData(paymentDetailsEventDetails.toString())
                .withEventType("PAYMENT_DETAILS_EVENT")
                .withEventDate(latestEventTime)
                .toEntity();

        when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(
                List.of(paymentCreatedEvent, paymentDetailsEvent));
        eventDigest = eventService.getEventDigestForResource(resourceExternalId);
    }

    @Test
    public void laterEventsShouldOverrideEarlierEventsInEventDetailsDigest() {
        JsonObject eventDetails = new JsonObject();
        eventDetails.addProperty("email", "new_email@example.com");
        Event latestEvent = EventFixture.anEventFixture()
                .withResourceExternalId(resourceExternalId)
                .withEventData(eventDetails.toString())
                .toEntity();

        when(mockEventDao.getEventsByResourceExternalId(resourceExternalId)).thenReturn(
                List.of(latestEvent, paymentCreatedEvent, paymentDetailsEvent));

        EventDigest eventDigest =  eventService.getEventDigestForResource(resourceExternalId);
        assertThat(eventDigest.getEventDetailsDigest().getEmail(), is("new_email@example.com"));
    }

    @Test
    public void shouldGetCorrectLatestSalientEventType() {
        assertThat(eventDigest.getMostRecentSalientEventType(), is(SalientEventType.PAYMENT_CREATED));
    }

    @Test
    public void shouldDeserialisePaymentCreatedEventCorrectly() {
        assertThat(eventDigest.getMostRecentSalientEventType(), is(SalientEventType.PAYMENT_CREATED));
        assertThat(eventDigest.getEventDetailsDigest().getAmount(), is(amount));
        assertThat(eventDigest.getEventDetailsDigest().getDescription(), is(description));
        assertThat(eventDigest.getEventDetailsDigest().getReference(), is(reference));
        assertThat(eventDigest.getEventDetailsDigest().getReturnUrl(), is(returnUrl));
        assertThat(eventDigest.getEventDetailsDigest().getGatewayAccountId(), is(gatewayAccountId));
        assertThat(eventDigest.getEventDetailsDigest().getPaymentProvider(), is(paymentProvider));
    }

    @Test
    public void shouldDeserialisePaymentDetailsEventCorrectly() {
        assertThat(eventDigest.getEventDetailsDigest().getEmail(), is(email));
        assertThat(eventDigest.getEventDetailsDigest().getCardholderName(), is(cardholderName));
        assertThat(eventDigest.getEventDetailsDigest().getAddressLine1(), is(addressLine1));
        assertThat(eventDigest.getEventDetailsDigest().getAddressLine2(), is(addressLine2));
        assertThat(eventDigest.getEventDetailsDigest().getAddressPostcode(), is(postcode));
        assertThat(eventDigest.getEventDetailsDigest().getAddressCity(), is(city));
        assertThat(eventDigest.getEventDetailsDigest().getAddressCounty(), is(county));
        assertThat(eventDigest.getEventDetailsDigest().getAddressCountry(), is(country));
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulCreatedResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.of(1L));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.INSERTED));
    }

    @Test
    public void createIfDoesNotExistReturnsSuccessfulIgnoredResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event)).thenReturn(Optional.empty());

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertTrue(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.IGNORED));
    }

    @Test
    public void createIfDoesNotExistReturnsNotSuccessfulResponse() {
        when(mockEventDao.insertEventIfDoesNotExistWithResourceTypeId(event))
                .thenThrow(new RuntimeException("forced failure"));

        CreateEventResponse response = eventService.createIfDoesNotExist(event);

        assertFalse(response.isSuccessful());
        assertThat(response.getState(), is(CreateEventResponse.CreateEventState.ERROR));
        assertThat(response.getErrorMessage(), is("forced failure"));
    }
}