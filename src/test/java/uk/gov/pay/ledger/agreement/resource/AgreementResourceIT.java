package uk.gov.pay.ledger.agreement.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.util.fixture.AgreementFixture;
import uk.gov.pay.ledger.util.fixture.PaymentInstrumentFixture;

import javax.ws.rs.core.Response;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;

public class AgreementResourceIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    @BeforeEach
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldGetAgreement() {
        var fixture = AgreementFixture.anAgreementFixture("a-valid-agreement-id", "a-valid-service-id")
            .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .get("/v1/agreement/a-valid-agreement-id")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("external_id", is(fixture.getExternalId()));
    }

    @Test
    public void shouldGetAgreementWithPaymentInstrument() {
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
                .body("payment_instrument.external_id", is(paymentInstrumentFixture.getExternalId()));
    }

    @Test
    public void shouldSearchEmptyWithNoAgreements() {
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
    public void shouldSearchWithPaginationForAgreements() {
        AgreementFixture.anAgreementFixture("a-valid-agreement-id", "a-valid-service-id")
                .insert(rule.getJdbi());
        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-valid-service-id")
                .get("/v1/agreement")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("total", is(1))
                .body("count", is(1))
                .body("page", is(1))
                .body("results.size()", is(1))
                .body("results[0].external_id", is("a-valid-agreement-id"));
    }

    @Test
    public void shouldSearchWithFilterParams() {
        AgreementFixture.anAgreementFixture("a-one-agreement-id", "a-one-service-id", "CREATED", "partial-ref-1").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-two-agreement-id", "a-one-service-id", "CREATED", "notmatchingref").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-three-agreement-id", "a-one-service-id", "ACTIVE", "anotherref").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-four-agreement-id", "a-two-service-id", "CREATED", "reference").insert(rule.getJdbi());
        AgreementFixture.anAgreementFixture("a-five-agreement-id", "a-one-service-id", "CREATED", "avalid-partial-ref").insert(rule.getJdbi());
        given().port(port)
                .contentType(JSON)
                .queryParam("service_id", "a-one-service-id")
                .queryParam("status", "CREATED")
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
}