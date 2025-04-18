# pay-ledger

This is a bare bones Dropwizard App for the Pay Ledger microservice.

## API Specification

The [API Specification](/openapi/ledger_spec.yaml) provides more detail on the paths and operations including examples.

[View the API specification for ledger in Swagger Editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/alphagov/pay-ledger/master/openapi/ledger_spec.yaml).

## Environment variables

There are several environment variables used for the app configuration. They're grouped in categories: database, SQS
and background processing.

| Variable | Default | Purpose                           |
|----------|---------|-----------------------------------|
| `BIND_HOST` | `127.0.0.1` | The IP address for the application to bind to |
| `PORT`   | -       | Port on which application listens |

### Database configuration

| Variable        | Default  | Purpose                                                                                      |
|-----------------|----------|----------------------------------------------------------------------------------------------|
| `DB_USER`       | -        | Name of the user used to connect to the database                                             |
| `DB_PASSWORD`   | -        | Password of the user used to connect to the database                                         |
| `DB_HOST`       | -        | Database host name                                                                           |
| `DB_NAME`       | `ledger` | Name of the database                                                                         |
| `DB_SSL_OPTION` | -        | Indicates whether the connection to the database should be secured with SSL (eg. `ssl=true`) |

### AWS Credentials Configuration

| Variable                                       | Default | Purpose                                                                                                                     |
|------------------------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
| `AWS_ACCESS_KEY`                               | -       | Access key. Only required when `AWS_SQS_NON_STANDARD_SERVICE_ENDPOINT` or `AWS_SNS_NON_STANDARD_SERVICE_ENDPOINT` is `true` |
| `AWS_SECRET_KEY`                               | -       | Secret key. Only required when `AWS_SQS_NON_STANDARD_SERVICE_ENDPOINT` or `AWS_SNS_NON_STANDARD_SERVICE_ENDPOINT` is `true` |

### SQS configuration

| Variable                                       | Default | Purpose                                                                                                    |
|------------------------------------------------|---------|------------------------------------------------------------------------------------------------------------|
| `AWS_SQS_REGION`                               | -       | SQS region                                                                                                 |
| `AWS_SQS_PAYMENT_EVENT_QUEUE_URL`              | -       | SQS payment event queue URL                                                                                |
| `AWS_SQS_MESSAGE_MAXIMUM_WAIT_TIME_IN_SECONDS` | `20`    | Maximum wait time for long poll message requests to queue                                                  |
| `AWS_SQS_MESSAGE_MAXIMUM_BATCH_SIZE`           | `10`    | Maximum number of messages that should be received in an individual message batch                          |
| `AWS_SQS_NON_STANDARD_SERVICE_ENDPOINT`        | `false` | Set to true to use a non standard (eg: `http://my-own-sqs-endpoint`) SQS endpoint                          |
| `AWS_SQS_ENDPOINT`                             | -       | URL that is the API endpoint for SQS. Only required when `AWS_SQS_NON_STANDARD_SERVICE_ENDPOINT` is `true` |

### SNS Configuration

| Variable                                       | Default | Purpose                                                                                                    |
|------------------------------------------------|---------|------------------------------------------------------------------------------------------------------------|
| `SNS_ENABLED`                                  | `false` | Set to true to send events to SNS                                                                          |
| `AWS_SNS_REGION`                               | -       | SNS region                                                                                                 |
| `AWS_SNS_NON_STANDARD_SERVICE_ENDPOINT`        | `false` | Set to true to use a non standard (eg: `http://my-own-sns-endpoint`) SNS endpoint                          |
| `AWS_SNS_ENDPOINT`                             | -       | URL that is the API endpoint for SNS. Only required when `AWS_SNS_NON_STANDARD_SERVICE_ENDPOINT` is `true` |
| `PUBLISH_CARD_PAYMENT_EVENTS_TO_SNS`           | `false` | Set to true to publish card payment events to the card payment events SNS topic                            |
| `PUBLISH_CARD_PAYMENT_DISPUTE_EVENTS_TO_SNS`   | `false` | Set to true to publish card payment dispute events to the card payment dispute events SNS topic            |
| `SNS_TOPIC_CARD_PAYMENT_EVENTS_ARN`            | -       | ARN of the SNS topic for card payment events                                                               |
| `SNS_TOPIC_CARD_PAYMENT_DISPUTE_EVENTS_ARN`    | -       | ARN of the SNS topic for card payment dispute events                                                       |

### Expunge/Redact historic data configuration

| Variable                                     | Default          | Purpose                                                                         |
|----------------------------------------------|------------------|---------------------------------------------------------------------------------|
| `EXPUNGE_AND_REDACT_HISTORICAL_DATA_ENABLED` | `false`          | Set to true to enable redacting PII from transactions and remove related events |
| `EXPUNGE_OR_REDACT_DATA_OLDER_THAN_DAYS`     | `2555` (7 years) | Minimum age of transactions in days that need to be redacted/expunged           |
| `EXPUNGE_NO_OF_TRANSACTIONS_PER_TASK_RUN`    | `2500`           | Number of transactions to redact per task run                                   |


### Background processing configuration

The background receiver will process all events that have been published to the SQS
queue&nbsp;(`AWS_SQS_PAYMENT_EVENT_QUEUE_URL`).

A background thread managed by Dropwizard runs on all connector nodes. It polls the SQS payment event queue to retrieve
the list of events.
Processing each event involves adding a record to `Event` database table&nbsp;(unless the event is a duplicate of an event
already processed, then the event is ignored).

If processing event fails it will be retried again after a specified delay (`QUEUE_MESSAGE_RETRY_DELAY_IN_SECONDS`).
It is achieved by setting up the visibility timeout with the delay value which prevents consumers from receiving the message.
After this timeout the message becomes visible for consumers again.
More information of how the visibility timeout works can be found [here](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html).

The following variables control the background process:

| Variable                                              | Default | Purpose                                                                                 |
|-------------------------------------------------------|---------|-----------------------------------------------------------------------------------------|
| `QUEUE_MESSAGE_RECEIVER_THREAD_DELAY_IN_MILLISECONDS` | `1`     | Duration in seconds that the queue message receiver should wait between running threads |
| `QUEUE_MESSAGE_RECEIVER_NUMBER_OF_THREADS`            | `1`     | The number of polling threads started by the queue message scheduler                    |
| `QUEUE_MESSAGE_RETRY_DELAY_IN_SECONDS`                | `900`   | The duration in seconds that a message should be deferred before it should be retried   |

## Licence

[MIT License](LICENSE)

## Responsible Disclosure

## Vulnerability Disclosure

GOV.UK Pay aims to stay secure for everyone. If you are a security researcher and have discovered a security vulnerability in this code, we appreciate your help in disclosing it to us in a responsible manner. Please refer to our [vulnerability disclosure policy](https://www.gov.uk/help/report-vulnerability) and our [security.txt](https://vdp.cabinetoffice.gov.uk/.well-known/security.txt) file for details.

