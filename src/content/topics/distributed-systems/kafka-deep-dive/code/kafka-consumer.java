// application.yml
// spring.kafka.consumer:
//   group-id: charger
//   auto-offset-reset: earliest
//   enable-auto-commit: false       # Spring commits after the listener returns (AckMode.BATCH by default)
//   properties:
//     partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
//     max.poll.interval.ms: 300000  # processing one poll's records must finish within this
// spring.kafka.listener.concurrency: 4   # ≤ number of partitions

@Configuration
class KafkaErrorHandling {
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // After the last retry, publish the record (with exception headers) to "<topic>-dlt".
        var recoverer = new DeadLetterPublishingRecoverer(template);
        var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(500);
        backoff.setMultiplier(2.0);
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(InvalidPaymentException.class);   // straight to the DLT
        return handler;
    }
}

@Component
class PaymentListener {
    @KafkaListener(topics = "payment-requests")
    void on(PaymentRequested e) {
        payments.chargeOnce(e);   // idempotent (see next tab); throwing → retry via seek, then DLT
    }
    // Offsets for this batch are committed only after the listener returns normally.
}
