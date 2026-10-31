// A needs B and B needs A
@Service class OrderService   { OrderService(InvoiceService invoices) { … } }
@Service class InvoiceService { InvoiceService(OrderService orders)   { … } }
// → BeanCurrentlyInCreationException: "The dependencies of some of the beans form a
// cycle". Spring Boot 2.6+ forbids circular references by default, even with field
// injection (spring.main.allow-circular-references=true re-enables the old
// early-reference trick — don't).

// ✓ Fix the design: extract what both need into a third bean…
@Service class OrderService   { OrderService(PricingRules rules) { … } }
@Service class InvoiceService { InvoiceService(PricingRules rules) { … } }

// ✓ …or decouple the direction with an event
@Service class OrderService {
    private final ApplicationEventPublisher events;
    void complete(Order o) { events.publishEvent(new OrderCompleted(o.id())); }
}
@Service class InvoiceService {
    @EventListener void on(OrderCompleted e) { createInvoice(e.orderId()); }
}

// Last resort: @Lazy on one constructor parameter injects a proxy that resolves on first
// use.
