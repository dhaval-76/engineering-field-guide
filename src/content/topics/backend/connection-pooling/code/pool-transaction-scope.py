# ✗ The connection is held for the whole function — including a 500 ms call to another
# service.
def checkout_bad(engine, cart):
    with engine.begin() as conn:                          # connection borrowed here…
        order = orders.insert(conn, cart)
        # …and idle for 500 ms while we wait
        payment = payment_client.charge(order)
        orders.mark_paid(conn, order.id, payment.id)
    # …returned only now

# ✓ Keep transactions around database work only.
def checkout(engine, cart):
    with engine.begin() as conn:                          # short transaction
        order = orders.insert_pending(conn, cart)
    payment = payment_client.charge(order)                # no connection held
    with engine.begin() as conn:                          # short transaction
        return orders.mark_paid(conn, order.id, payment.id)
# (The order is PENDING between the two — design states and retries for that; see
# idempotency.)
