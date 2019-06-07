package uk.gov.pay.ledger.utils.fixtures;

import com.amazonaws.services.sqs.AmazonSQS;

public interface QueueFixture<F> {
    F insert(AmazonSQS sqsClient);
}
