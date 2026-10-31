// ✗ The connection is held for the whole function — including a 500 ms call to another
// service.
async function checkoutBad(pool, cart) {
  const client = await pool.connect(); // connection borrowed here…
  try {
    await client.query('BEGIN');
    const order = await orders.insert(client, cart);
    // …and idle for 500 ms while we wait
    const payment = await paymentClient.charge(order);
    await orders.markPaid(client, order.id, payment.id);
    await client.query('COMMIT');
  } finally {
    client.release(); // …returned only now
  }
}

// ✓ Keep transactions around database work only.
async function checkout(pool, cart) {
  // short transaction
  const order = await inTx(pool, (c) => orders.insertPending(c, cart));
  // no connection held
  const payment = await paymentClient.charge(order);
  // short transaction
  return inTx(pool, (c) => orders.markPaid(c, order.id, payment.id));
}

async function inTx(pool, work) {
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    const result = await work(c);
    await c.query('COMMIT');
    return result;
  } catch (e) {
    await c.query('ROLLBACK');
    throw e;
  } finally {
    c.release();
  }
}
// (The order is PENDING between the two — design states and retries for that; see
// idempotency.)
