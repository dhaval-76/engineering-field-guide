// ✗ The connection is held for the whole method — including a 500 ms call to another service
@Transactional
public Receipt checkout(Cart cart) {
    Order order = orders.save(Order.from(cart));          // connection borrowed here…
    PaymentResult p = paymentClient.charge(order);         // …and idle for 500 ms while we wait
    order.markPaid(p.id());
    return Receipt.of(order);                              // …returned only now
}

// ✓ Keep transactions around database work only
public Receipt checkout(Cart cart) {
    Order order = tx.execute(s -> orders.save(Order.pending(cart)));       // short transaction
    PaymentResult p = paymentClient.charge(order);                          // no connection held
    return tx.execute(s -> Receipt.of(orders.markPaid(order.id(), p.id()))); // short transaction
}
// (The order is PENDING between the two — design states and retries for that; see idempotency.)
