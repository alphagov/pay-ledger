package uk.gov.pay.ledger.nats.as_sns;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PushSubscribeOptions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SubscribeToJetstreamPaymentDisputeEvent {

    public static final String STREAM = "card_payment_topic";
    public static final String SUBJECT = "disputes";
    
    public static void main(String[] args) throws Exception {
        Options o = new Options.Builder().server("nats://localhost:60102")
                .userInfo("local", "pGEqNddjOog51XKGd0tCyqa4rk6TRCWa").build(); // pragma: allowlist secret
        try (Connection nc = Nats.connect(o)) {
            JetStream jsContext = nc.jetStream();

            PushSubscribeOptions pso = PushSubscribeOptions.builder()
                    .stream(STREAM)
                    .durable("ledger-subscriber") // it's okay if this is null, the builder handles it
                    .build();

            JetStreamSubscription jetStreamSubscription = jsContext.subscribe(SUBJECT, pso);

            while (true) {
                Message msg = jetStreamSubscription.nextMessage(Duration.ofSeconds(1));
                if (msg != null) {
                    String data = new String(msg.getData(), StandardCharsets.US_ASCII);
                    System.out.printf("Received message. Subject: %s, Data: %s\n", msg.getSubject(), data);
                    msg.ack();
                }
            }
        }

    }
}
