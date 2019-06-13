package uk.gov.pay.ledger.utils.fixtures;

import com.amazonaws.services.sqs.AmazonSQS;

public interface QueueFixture<F, E> {
    F insert(AmazonSQS sqsClient);

    E toEntity();
}
