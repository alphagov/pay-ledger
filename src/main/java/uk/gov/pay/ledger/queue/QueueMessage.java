package uk.gov.pay.ledger.queue;

import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.List;
import java.util.stream.Collectors;

public class QueueMessage {

    private String messageId;
    private String receiptHandle;
    private String messageBody;

    private QueueMessage(String messageId, String receiptHandle, String messageBody) {
        this.messageId = messageId;
        this.receiptHandle = receiptHandle;
        this.messageBody = messageBody;
    }

    private QueueMessage(String messageId, String messageBody) {
        this(messageId, null, messageBody);
    }

    public static List<QueueMessage> of(ReceiveMessageResult receiveMessageResult) {

        return receiveMessageResult.getMessages()
                .stream()
                .map(c -> new QueueMessage(c.getMessageId(), c.getReceiptHandle(), c.getBody()))
                .collect(Collectors.toList());
    }

    public static QueueMessage of(SendMessageResult messageResult, String validJsonMessage) {
        return new QueueMessage(messageResult.getMessageId(), validJsonMessage);
    }

    public String getMessageId() {
        return messageId;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }

    public String getMessageBody() {
        return messageBody;
    }
}
