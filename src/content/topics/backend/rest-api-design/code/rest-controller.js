// Express + zod: the same contract
const CreateOrder = z.object({
  customerId: z.string().min(1),
  items: z.array(z.object({ sku: z.string().min(1), quantity: z.number().int().positive() })).min(1),
  note: z.string().max(500).optional(),
});

app.post('/v1/orders', async (req, res) => {
  const parsed = CreateOrder.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).type('application/problem+json').json({
      type: 'https://api.example.com/problems/validation',
      title: 'Request validation failed',
      status: 400,
      errors: parsed.error.issues.map((i) => ({ field: i.path.join('.'), message: i.message })),
    });
  }
  const order = await orders.create(parsed.data, req.get('Idempotency-Key'));
  res.status(201).location(`/v1/orders/${order.id}`).json(order);
});
