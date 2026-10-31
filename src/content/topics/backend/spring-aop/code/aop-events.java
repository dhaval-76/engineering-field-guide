record OrderPlaced(String orderId) {}

@Service
class OrderService {
    private final ApplicationEventPublisher events;
    …
    @Transactional
    public Order place(OrderRequest req) {
        Order order = orders.save(Order.from(req));
        // delivered according to each listener's phase
        events.publishEvent(new OrderPlaced(order.id()));
        return order;
    }
}

@Component
class InvoiceListener {
    @Async                                                  // don't delay the caller
    // only if the order really committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderPlaced e) { invoices.createFor(e.orderId()); }
}
// Caveat: in-memory events are lost if the process dies after commit but before the
// listener runs. When the side effect must happen, write an outbox row in the same
// transaction and publish from it.
