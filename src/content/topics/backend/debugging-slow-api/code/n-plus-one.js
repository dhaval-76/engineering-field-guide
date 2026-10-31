// ✗ N+1: one query for orders, then one per order for its items → 501 queries for 500 orders
const orders = (await db.query('SELECT * FROM orders WHERE customer_id = $1', [customerId])).rows;
for (const o of orders) {
  o.items = (await db.query('SELECT * FROM order_items WHERE order_id = $1', [o.id])).rows;
}

// ✓ Two queries total: batch the children with = ANY($1) and group in memory
const ids = orders.map((o) => o.id);
const { rows: items } = await db.query('SELECT * FROM order_items WHERE order_id = ANY($1)', [ids]);
const byOrder = Map.groupBy(items, (i) => i.order_id);
orders.forEach((o) => (o.items = byOrder.get(o.id) ?? []));

// ✓ With an ORM, ask for the relation up front — Prisma: findMany({ where, include: { items: true } })
// ✓ For read endpoints, often best: one aggregate query with exactly the columns the response needs
const { rows: summaries } = await db.query(
  `SELECT o.id, o.created_at, count(i.*) AS items, sum(i.price * i.quantity) AS total
   FROM orders o JOIN order_items i ON i.order_id = o.id
   WHERE o.customer_id = $1 GROUP BY o.id, o.created_at`, [customerId]);
// Catch it in tests: count queries per request (wrap the pool's query function) and assert a small fixed number.
