package uk.gov.pay.ledger.agreement.resource;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.transaction.model.CardType;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.EventFixture;
import uk.gov.pay.ledger.util.fixture.PaymentInstrumentFixture;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;
import uk.gov.service.payments.commons.model.agreement.PaymentInstrumentType;

import javax.ws.rs.core.Response;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.agreement.resource.AgreementSearchParams.DEFAULT_DISPLAY_SIZE;

class AgreementResourceIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    @BeforeEach
    void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    void shouldGetAgreement() {
        var fixture = AgreementFixture.anAgreementFixture("a-valid-agreement-id", "a-valid-service-id");
        fixture.setUserIdentifier("a-valid-user-identifier");
        fixture.insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/agreement/a-valid-agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("external_id", is(fixture.getExternalId()))
                .body("user_identifier", is("a-valid-user-identifier"));
    }

    @Test
    void shouldGetAgreementWithPaymentInstrument() {
        var agreementFixture = AgreementFixture.anAgreementFixture("a-valid-agreement-id", "a-valid-service-id")
                .insert(rule.getJdbi());
        var paymentInstrumentFixture = PaymentInstrumentFixture.aPaymentInstrumentFixture("a-payment-instrument-id", "a-valid-agreement-id", ZonedDateTime.now())
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/agreement/a-valid-agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("external_id", is(agreementFixture.getExternalId()))
                .body("payment_instrument.external_id", is(paymentInstrumentFixture.getExternalId()))
                .body("payment_instrument.type", is(PaymentInstrumentType.CARD.name()))
                .body("payment_instrument.card_details.card_type", is(CardType.CREDIT.name().toLowerCase()))
                .body("payment_instrument.card_details.first_digits_card_number", is("424242"));
    }

    @Test
    void shouldSearchEmptyWithNoAgreements() {
        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-valid-service-id")
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(0))
                .body("count", is(0))
                .body("page", is(1))
                .body("results.size()", is(0));
    }

    @Test
    void shouldSearchWithPaginationForAgreements_defaultDisplaySize() {
        int numberOfAgreements = (int) Math.ceil(DEFAULT_DISPLAY_SIZE + (DEFAULT_DISPLAY_SIZE / 2));
        prepareAgreementsForService("a-valid-service-id", numberOfAgreements);
        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-valid-service-id")
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(numberOfAgreements))
                .body("count", is((int)DEFAULT_DISPLAY_SIZE))
                .body("page", is(1))
                .body("results.size()", is((int)DEFAULT_DISPLAY_SIZE))
                .body("_links.self.href", containsString("v1/agreement?service_id=a-valid-service-id&page=1&display_size=20"))
                .body("_links.first_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=1&display_size=20"))
                .body("_links.last_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=2&display_size=20"))
                .body("_links.prev_page", is(nullValue()))
                .body("_links.next_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=2&display_size=20"));

        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-valid-service-id")
                .queryParam("page", 2)
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(numberOfAgreements))
                .body("count", is((int)(numberOfAgreements - DEFAULT_DISPLAY_SIZE)))
                .body("page", is(2))
                .body("results.size()", is((int)(numberOfAgreements - DEFAULT_DISPLAY_SIZE)))
                .body("_links.self.href", containsString("v1/agreement?service_id=a-valid-service-id&page=2&display_size=20"))
                .body("_links.first_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=1&display_size=20"))
                .body("_links.last_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=2&display_size=20"))
                .body("_links.prev_page.href", containsString("v1/agreement?service_id=a-valid-service-id&page=1&display_size=20"))
                .body("_links.next_page", is(nullValue()));
    }

    @Test
    void shouldSearchWithPaginationForAgreements_withCustomDisplaySize() {
        prepareAgreementsForService("a-valid-service-id", 3);

        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-valid-service-id")
                .queryParam("display_size", 2)
                .queryParam("page", 2)
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(3))
                .body("count", is(1))
                .body("page", is(2))
                .body("results.size()", is(1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "created", "CREATED" })
    void shouldSearchWithFilterParams(String searchStatus) {
        AgreementFixture.anAgreementFixture("a-one-agreement-id", "a-one-service-id", AgreementStatus.CREATED, "partial-ref-1").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-two-agreement-id", "a-one-service-id", AgreementStatus.CREATED, "notmatchingref").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-three-agreement-id", "a-one-service-id", AgreementStatus.ACTIVE, "anotherref").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-four-agreement-id", "a-two-service-id", AgreementStatus.CREATED, "reference").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-five-agreement-id", "a-one-service-id", AgreementStatus.CREATED, "avalid-partial-ref").insert(rule.getJdbi());
        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-one-service-id")
                .queryParam("status", searchStatus)
                .queryParam("reference", "partial-ref")
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(2))
                .body("count", is(2))
                .body("page", is(1))
                .body("results.size()", is(2))
                .body("results[0].external_id", is("a-five-agreement-id"))
                .body("results[1].external_id", is("a-one-agreement-id"));
    }

    @Test
    void shouldGetConsistentAgreement_GetExistingProjectionWhenUpToDate() {
        var agreementFixture = AgreementFixture.anAgreementFixture("agreement-id", "service-id", AgreementStatus.CREATED, "projected-agreement-reference");
        agreementFixture.setEventCount(1);
        agreementFixture.insert(rule.getJdbi());

        EventFixture.anEventFixture()
                .withResourceExternalId("agreement-id")
                .withServiceId("service-id")
                .withEventType("AGREEMENT_CREATED")
                .withEventData(
                        new JSONObject()
                                .put("reference", "event-stream-reference")
                                .put("status", "CREATED")
                                .toString()
                )
                .insert(rule.getJdbi());

        given()
                .port(port)
                .contentType(JSON)
                .header("X-Consistent", true)
                .get("/v1/agreement/agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("external_id", is("agreement-id"))
                .body("reference", is("projected-agreement-reference"));
    }

    @Test
    void shouldGetConsistentAgreement_GetEventStreamCalculatedWhenProjectionCountBehind() {
        var agreementFixture = AgreementFixture.anAgreementFixture("agreement-id", "service-id", AgreementStatus.CREATED, "projected-agreement-reference");
        agreementFixture.setEventCount(1);
        agreementFixture.insert(rule.getJdbi());

        EventFixture.anEventFixture()
                .withResourceExternalId("agreement-id")
                .withServiceId("service-id")
                .withEventType("AGREEMENT_CREATED")
                .withEventData(
                        new JSONObject()
                                .put("reference", "event-stream-reference")
                                .put("status", "CREATED")
                                .toString()
                )
                .insert(rule.getJdbi());

        EventFixture.anEventFixture()
                .withResourceExternalId("agreement-id")
                .withServiceId("service-id")
                .withEventType("AGREEMENT_SETUP")
                .withEventData(
                        new JSONObject()
                                .put("status", "ACTIVE")
                                .toString()
                )
                .insert(rule.getJdbi());

        given()
                .port(port)
                .contentType(JSON)
                .header("X-Consistent", true)
                .get("/v1/agreement/agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("external_id", is("agreement-id"))
                .body("reference", is("event-stream-reference"))
                .body("status", is("ACTIVE"));
    }

    @Test
    void shouldGetConsistentAgreement_GetEventStreamCalculatedWhenProjectionMissing() {
        EventFixture.anEventFixture()
                .withResourceExternalId("agreement-id")
                .withServiceId("service-id")
                .withEventType("AGREEMENT_CREATED")
                .withEventData(
                        new JSONObject()
                                .put("reference", "event-stream-reference")
                                .put("status", "CREATED")
                                .toString()
                )
                .insert(rule.getJdbi());

        given()
                .port(port)
                .contentType(JSON)
                .header("X-Consistent", true)
                .get("/v1/agreement/agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("external_id", is("agreement-id"))
                .body("reference", is("event-stream-reference"));
    }

    @Test
    void shouldGetConsistentAgreement_404GivenNoProjectionOrEvents() {
        given()
                .port(port)
                .contentType(JSON)
                .header("X-Consistent", true)
                .get("/v1/agreement/agreement-id")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    private void prepareAgreementsForService(String serviceId, int numberOfAgreements) {
        for (int i = 0; i < numberOfAgreements; i++) {
            AgreementFixture.anAgreementFixture("a-valid-external-id-" + i, serviceId)
                    .insert(rule.getJdbi());
        }
    }
}