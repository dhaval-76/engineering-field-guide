// ✗ The connection is held for the whole method — including a 500 ms call to another
// service
@Transactional
public Receipt checkout(Cart cart) {
    Order order = orders.save(Order.from(cart));          // connection borrowed here…
    // …and idle for 500 ms while we wait
    PaymentResult p = paymentClient.charge(order);
    order.markPaid(p.id());
    return Receipt.of(order);                              // …returned only now
}

// ✓ Keep transactions around database work only
public Receipt checkout(Cart cart) {
    // short transaction
    Order order = tx.execute(s -> orders.save(Order.pending(cart)));
    // no connection held
    PaymentResult p = paymentClient.charge(order);
    // short transaction
    return tx.execute(s -> Receipt.of(orders.markPaid(order.id(), p.id())));
}
// (The order is PENDING between the two — design states and retries for that; see
// idempotency.)
