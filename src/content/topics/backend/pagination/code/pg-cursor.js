const encode = (row) => Buffer.from(JSON.stringify([row.created_at, row.id])).toString('base64url');
const decode = (s) => JSON.parse(Buffer.from(s, 'base64url').toString());

app.get('/v1/customers/:customerId/orders', async (req, res) => {
  const limit = Math.min(Number(req.query.limit ?? 20), 100);
  const params = [req.params.customerId];
  let where = 'customer_id = $1';
  if (req.query.cursor) {
    const [t, id] = decode(req.query.cursor);
    params.push(t, id);
    where += ' AND (created_at, id) < ($2, $3)';
  }
  params.push(limit + 1);
  const { rows } = await pool.query(
    `SELECT id, created_at, total FROM orders WHERE ${where}
     ORDER BY created_at DESC, id DESC LIMIT $${params.length}`, params);

  const page = rows.slice(0, limit);
  res.json({ data: page, nextCursor: rows.length > limit ? encode(page.at(-1)) : null });
});
