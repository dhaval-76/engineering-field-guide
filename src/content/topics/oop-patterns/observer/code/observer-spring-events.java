public record OrderPlaced(String orderId, String customerId, BigDecimal total) {}

@Service
class OrderService {
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    OrderService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders; this.events = events;
    }

    @Transactional
    public Order place(PlaceOrder cmd) {
        Order order = orders.save(Order.from(cmd));
        events.publishEvent(new OrderPlaced(order.id(), order.customerId(), order.total()));
        // knows nothing about email, invoices, analytics
        return order;
    }
}

@Component
class ConfirmationEmail {
    // Runs only if the transaction commits — no emails for rolled-back orders.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void on(OrderPlaced e) { mailer.sendConfirmation(e.customerId(), e.orderId()); }
}

@Component
class InvoiceGenerator {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(OrderPlaced e) { invoices.create(e.orderId(), e.total()); }
}
