package uk.gov.pay.ledger.payout.service;

import com.google.inject.Inject;
import uk.gov.pay.ledger.event.model.EventDigest;
import uk.gov.pay.ledger.payout.dao.PayoutDao;
import uk.gov.pay.ledger.payout.entity.PayoutEntity;
import uk.gov.pay.ledger.payout.model.PayoutEntityFactory;

public class PayoutService {

    private final PayoutDao payoutDao;
    private final PayoutEntityFactory payoutEntityFactory;

    @Inject
    public PayoutService(PayoutDao payoutDao, PayoutEntityFactory payoutEntityFactory) {
        this.payoutDao = payoutDao;
        this.payoutEntityFactory = payoutEntityFactory;
    }

    public void upsertPayoutFor(EventDigest eventDigest) {
        PayoutEntity payoutEntity = payoutEntityFactory.create(eventDigest);
        payoutDao.upsert(payoutEntity);
    }
}
