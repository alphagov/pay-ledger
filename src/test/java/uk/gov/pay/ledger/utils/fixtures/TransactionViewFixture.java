package uk.gov.pay.ledger.utils.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.ledger.transaction.model.CardDetails;
import uk.gov.pay.ledger.transaction.search.model.TransactionView;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionViewFixture implements DbFixture<TransactionViewFixture, TransactionView> {

    private Long id = RandomUtils.nextLong(1, 99999);;
    private String gatewayAccountId = RandomStringUtils.randomAlphanumeric(20);
    private Long amount = 1000L;
    private TransactionState state = TransactionState.CREATED;
    private String description = "a description";
    private String reference = "areference";
    private String language = "en";
    private String externalId = RandomStringUtils.randomAlphanumeric(32);
    private String returnUrl = "https://example.org/transactions";
    private String email = "j.doe@example.org";
    private String paymentProvider = "sandbox";
    private ZonedDateTime createdDate = ZonedDateTime.now(ZoneOffset.UTC);
    private CardDetails cardDetails = null;
    private Boolean delayedCapture = false;

    public static TransactionViewFixture aTransactionViewList() {
        return new TransactionViewFixture();
    }
    public TransactionViewFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public TransactionViewFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public TransactionViewFixture withAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public TransactionViewFixture withState(TransactionState state) {
        this.state = state;
        return this;
    }

    public TransactionViewFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public TransactionViewFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public TransactionViewFixture withLanguage(String language) {
        this.language = language;
        return this;
    }

    public TransactionViewFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public TransactionViewFixture withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }

    public TransactionViewFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public TransactionViewFixture withPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
        return this;
    }

    public TransactionViewFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public TransactionViewFixture withCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        return this;
    }

    public TransactionViewFixture withDelayedCapture(Boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return this;
    }

    public static List<TransactionView> aTransactionViewList(String gatewayAccountId, int noOfViews) {
        List<TransactionView> transactionViewList = new ArrayList<>();
        for (int i = 0; i < noOfViews; i++) {
            transactionViewList.add(aTransactionViewList()
                                        .withGatewayAccountId(gatewayAccountId)
                                        .toEntity());
        }
        return transactionViewList;
    }

    @Override
    public TransactionViewFixture insert(Jdbi jdbi) {
        return null;
    }

    @Override
    public TransactionView toEntity() {
        return new TransactionView(this.id, this.gatewayAccountId, this.amount, this.state,
                this.description, this.reference, this.language, this.externalId,
                this.returnUrl, this.email, this.paymentProvider, this.createdDate,
                this.cardDetails, this.delayedCapture);
    }
}
