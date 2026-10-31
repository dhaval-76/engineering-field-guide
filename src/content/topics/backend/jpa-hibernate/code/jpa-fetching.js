// Prisma: relations are never loaded implicitly, so N+1 only happens if you loop
// yourself.

// ✗ N+1: one query for orders, then one per order
const orders = await prisma.order.findMany({ where: { customerId } });
for (const o of orders) {
  o.lines = await prisma.orderLine.findMany({ where: { orderId: o.id } });
}

// ✓ include: Prisma loads the relation for all orders with one extra query (WHERE
// order_id IN …)
const withLines = await prisma.order.findMany({
  where: { customerId },
  include: { lines: true, customer: { select: { name: true } } },
});

// ✓ select: a projection — only the fields the screen needs
const summaries = await prisma.order.findMany({
  select: { id: true, status: true, _count: { select: { lines: true } } },
  take: 20,                     // pagination is applied in SQL, even with relations
  orderBy: { id: 'desc' },
});
