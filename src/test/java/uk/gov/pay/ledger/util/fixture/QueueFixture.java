package uk.gov.pay.ledger.util.fixture;

import software.amazon.awssdk.services.sqs.SqsClient;

public interface QueueFixture<F, E> {
    F insert(SqsClient sqsClient);

    E toEntity();
}
