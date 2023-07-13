package uk.gov.pay.ledger.nats.as_sqs;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import java.nio.charset.StandardCharsets;

public class SubscribeToJetstreamPaymentEventQueueTwo {

    public static void main(String[] args) throws Exception {
        Options o = new Options.Builder().server("nats://localhost:60102")
                .userInfo("local", "pGEqNddjOog51XKGd0tCyqa4rk6TRCWa").build(); // pragma: allowlist secret
        try (Connection nc = Nats.connect(o)) {
            JetStream jsContext = nc.jetStream();

            Dispatcher dispatcher = nc.createDispatcher();
            
            PushSubscribeOptions pso = ConsumerConfiguration.builder()
                    .durable("ledger-subscriber")
                    .deliverGroup("payment-event-queue")
                    .buildPushSubscribeOptions();

            while (true) {
                jsContext.subscribe("payment.event", dispatcher, msg -> {
                    String data = new String(msg.getData(), StandardCharsets.US_ASCII);
                    System.out.printf("Received message: %s\n", data);
                }, true, pso);
            }
        }
        
    }
}
