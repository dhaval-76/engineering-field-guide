// Vitest + Testcontainers: a real PostgreSQL in Docker, not an in-memory imitation.
import { beforeAll, afterAll, it, expect } from 'vitest';
import { PostgreSqlContainer } from '@testcontainers/postgresql';
import pg from 'pg';
import { migrate } from './migrate.js';
import { OrderRepository } from './order-repository.js';

let container, pool, repo;

beforeAll(async () => {
  container = await new PostgreSqlContainer('postgres:17-alpine').start(); // ~2–5 s, once per file
  pool = new pg.Pool({ connectionString: container.getConnectionUri() });
  await migrate(pool);                                                     // the same migrations as production
  repo = new OrderRepository(pool);
}, 60_000);

afterAll(async () => {
  await pool?.end();
  await container?.stop();
});

it('loads an order with its lines', async () => {
  const customerId = await repo.createCustomer('Asha');
  await repo.createOrder(customerId, [{ sku: 'BOOK', qty: 2 }, { sku: 'PEN', qty: 1 }]);

  const [order] = await repo.findWithLines(customerId);

  expect(order.lines.map((l) => l.sku).sort()).toEqual(['BOOK', 'PEN']);
});

it('refuses to reserve more stock than exists', async () => {
  const productId = await repo.createProduct('BOOK', 1);
  expect(await repo.reserve(productId, 2)).toBe(0);                        // the atomic UPDATE refuses
});
