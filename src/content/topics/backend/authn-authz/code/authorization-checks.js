// Express: authorization in middleware and in the queries — never trust ids from the client alone.
// req.user comes from requireAuth (JWT verification).
export const requireRole = (...roles) => (req, res, next) =>
  roles.some((r) => req.user.roles.includes(r)) ? next() : res.status(403).end();

// Role check: who may call this at all
app.delete('/orders/:id', requireAuth, requireRole('ADMIN'), async (req, res) => {
  await orders.deleteById(req.params.id);
  res.status(204).end();
});

// Ownership check: the data decides — compare the resource with the caller
app.get('/orders/:id', requireAuth, async (req, res) => {
  const order = await orders.view(req.params.id);
  const allowed = order && (order.customerId === req.user.id || req.user.roles.includes('SUPPORT'));
  if (!allowed) return res.status(404).end(); // 404, not 403: don't reveal that someone else's order exists
  res.json(order);
});

// Better for lists: scope the QUERY to the caller (never findAll() then filter)
app.get('/me/orders', requireAuth, async (req, res) => {
  res.json(await orders.findByCustomerId(req.user.id, req.query.cursor));
});
// Test all three: no token → 401; valid token without permission → 403/404; someone else's id → 404.
