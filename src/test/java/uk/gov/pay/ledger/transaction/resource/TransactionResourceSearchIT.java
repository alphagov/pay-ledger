package uk.gov.pay.ledger.transaction.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.ledger.rule.AppWithPostgresAndSqsRule;
import uk.gov.pay.ledger.transaction.search.model.RefundSummary;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static uk.gov.pay.ledger.util.DatabaseTestHelper.aDatabaseTestHelper;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class TransactionResourceSearchIT {

    @ClassRule
    public static AppWithPostgresAndSqsRule rule = new AppWithPostgresAndSqsRule();

    private Integer port = rule.getAppRule().getLocalPort();

    @Before
    public void setUp() {
        aDatabaseTestHelper(rule.getJdbi()).truncateAllData();
    }

    @Test
    public void shouldSearchCorrectlyOnTransactionAndParentTransaction_WhenWithParentTransactionIsTrue() {
        String gatewayAccountId = "1570";

        new GsonBuilder().create()
                .toJson(ImmutableMap.of("external_metadata", "metadata"));

        String transactionDetails = "{\"fee\": 52, \"live\": true, \"moto\": false, \"email\": \"abc@hotmail.co.uk\", " +
                "\"amount\": 8800, \"source\": \"CARD_API\", \"language\": \"en\", \"card_type\": \"DEBIT\", \"reference\": \"FFG-670-20-2222\"," +
                " \"card_brand\": \"visa\", \"net_amount\": 8748, \"return_url\": \"http://url\", \"description\": \"application\", \"expiry_date\": \"09/22\", " +
                "\"address_city\": \"city\", \"total_amount\": 8800, \"address_line1\": \"line1\", \"address_line2\": \"line2\", \"captured_date\": \"2020-05-20T19:57:54.817135Z\", " +
                "\"refund_status\": \"available\", \"address_country\": \"GB\", \"cardholder_name\": \"name\", \"delayed_capture\": false, " +
                "\"address_postcode\": \"HR136NQ\", \"card_brand_label\": \"Visa\", \"payment_provider\": \"stripe\", \"gateway_payout_id\": \"payoutid\", " +
                "\"gateway_account_id\": \"1570\", \"capture_submitted_date\": \"2020-05-20T19:57:54.817135Z\", \"gateway_transaction_id\": \"transactionid\", " +
                "\"refund_amount_refunded\": 0, \"last_digits_card_number\": \"1234\", \"refund_amount_available\": 8800, \"first_digits_card_number\": \"000000\"}";

        RefundSummary refundSummary = new RefundSummary("available", 8800L, 0L);
        TransactionFixture payment = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("PAYMENT")
                .withExternalId("sncg9eh46hvqrrb7tvih2hbn8d")
                .withAmount(8800L)
                .withReference("FFG-670-20-2222")
                .withDescription("application")
                .withEmail("abc@hotmail.co.uk")
                .withCardholderName("name")
                .withCreatedDate(ZonedDateTime.parse("2020-05-20T19:53:08.094414Z"))
                .withEventCount(10)
                .withCardBrand("visa")
                .withLastDigitsCardNumber("1234")
                .withFirstDigitsCardNumber("000000")
                .withNetAmount(8748)
                .withTotalAmount(8800)
                .withCapturedDate(null)
                .withCaptureSubmittedDate(null)
                .withRefundSummary(refundSummary)
                .withFee(52L)
                .withGatewayTransactionId("transactionid")
                .withParentExternalId(null)
                .withLive(true)
                .withSource("CARD_API")
                .withMoto(false)
                .withState(TransactionState.SUCCESS)
                .withGatewayPayoutId("payoutid")
                .withTransactionDetails(transactionDetails)
                .insert(rule.getJdbi());

        TransactionFixture refund = aTransactionFixture()
                .withGatewayAccountId(gatewayAccountId)
                .withTransactionType("REFUND")
                .withExternalId("f2mqa2t76gjgd923j5i4s4kcu0")
                .withAmount(8800L)
                .withReference("re_1GlC3dDv3CZEaFO2SRfo3DSA")
                .withCreatedDate(ZonedDateTime.parse("2020-05-21T10:55:25.094414Z"))
                .withState(TransactionState.SUCCESS)
                .withParentExternalId(payment.getExternalId())
                .withGatewayPayoutId("po_1GlPIHACnWImrXevMuWT4oaQ")
                .insert(rule.getJdbi());

        given().port(port)
                .contentType(JSON)
                .accept(JSON)
                .get("/v1/transaction?" +
                        "account_id=" + gatewayAccountId +
                        "&with_parent_transaction=true&override_account_id_restriction=false&status_version=2&display_size=39"
                )
                .then()
                .log().all();
    }

}
