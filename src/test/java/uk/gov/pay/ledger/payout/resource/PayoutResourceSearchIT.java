package uk.gov.pay.ledger.payout.resource;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.ledger.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;

import javax.ws.rs.core.Response;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.PayoutFixture.aPersistedPayoutList;

public class PayoutResourceSearchIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();

    private Integer port = rule.getAppRule().getLocalPort();

    @BeforeEach
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllPayoutData();
    }

    @Test
    public void shouldSearchUsingAllFieldsAndReturnAllFieldsCorrectly() {
        String gatewayAccountId = RandomStringUtils.randomAlphanumeric(20);
        List<PayoutEntity> entityList = aPersistedPayoutList(gatewayAccountId, 10, rule.getJdbi());
        PayoutEntity payoutToVerify = entityList.get(7);
        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/payout?" +
                        "gateway_account_id=" + gatewayAccountId +
                        "&page=2" +
                        "&display_size=2" +
                        "&state=paidout"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("results[0].gateway_account_id", is(payoutToVerify.getGatewayAccountId()))
                .body("results[0].gateway_payout_id", is(payoutToVerify.getGatewayPayoutId()))
                .body("results[0].created_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(payoutToVerify.getCreatedDate())))
                .body("results[0].paid_out_date", is(ISO_INSTANT_MILLISECOND_PRECISION.format(payoutToVerify.getPaidOutDate())))
                .body("results[0].state.finished", is(true))
                .body("results[0].state.status", is("paidout"))
                .body("count", is(2))
                .body("page", is(2))
                .body("total", is(10))
                .body("_links.self.href", containsString("v1/payout?gateway_account_id=" + gatewayAccountId + "&state=paidout&page=2&display_size=2"))
                .body("_links.first_page.href", containsString("v1/payout?gateway_account_id=" + gatewayAccountId + "&state=paidout&page=1&display_size=2"))
                .body("_links.last_page.href", containsString("v1/payout?gateway_account_id=" + gatewayAccountId + "&state=paidout&page=5&display_size=2"))
                .body("_links.prev_page.href", containsString("v1/payout?gateway_account_id=" + gatewayAccountId + "&state=paidout&page=1&display_size=2"))
                .body("_links.next_page.href", containsString("v1/payout?gateway_account_id=" + gatewayAccountId + "&state=paidout&page=3&display_size=2"));
    }

    @Test
    public void shouldAllowNotSupplyingGatewayAccountId() {
        String gatewayAccountId = "123";
        String otherGatewayAccountId = "456";

        aPersistedPayoutList(gatewayAccountId, 2, rule.getJdbi());
        aPersistedPayoutList(otherGatewayAccountId, 2, rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/payout?" +
                        "override_account_id_restriction=true"
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(4))
                .body("results[0].gateway_account_id", is(otherGatewayAccountId))
                .body("results[2].gateway_account_id", is(gatewayAccountId));
    }

    @Test
    public void shouldSearchUsingMultipleGatewayAccountIds() {
        String gatewayAccountId = "123";
        String gatewayAccountId2 = "456";
        String gatewayAccountId3 = "1337";

        aPersistedPayoutList(gatewayAccountId, 2, rule.getJdbi());
        aPersistedPayoutList(gatewayAccountId2, 2, rule.getJdbi());
        aPersistedPayoutList(gatewayAccountId3, 2, rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/payout?" +
                        "override_account_id_restriction=true" +
                        "&gateway_account_id=" + gatewayAccountId + "," + gatewayAccountId3
                )
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(JSON)
                .body("count", is(4))
                .body("results[0].gateway_account_id", is(gatewayAccountId3))
                .body("results[2].gateway_account_id", is(gatewayAccountId));
    }
}