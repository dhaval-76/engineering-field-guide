// Reproduce exhaustion: pool of 2, ten requests that each hold a connection for 2 s, wait
// limit 3 s.
import pg from 'pg';

const pool = new pg.Pool({ max: 2, connectionTimeoutMillis: 3000 });

const results = await Promise.all(
  Array.from({ length: 10 }, async () => {
    const start = Date.now();
    try {
      const c = await pool.connect();
      try {
        await c.query('SELECT pg_sleep(2)');
      } finally {
        c.release();
      }
      return `ok after ${Date.now() - start} ms`;
    } catch {
      return 'timed out waiting for a connection';
    }
  }),
);
console.log(results.join('\n'));
await pool.end();
// Expect: 2 finish after ~2 s, 2 after ~4 s (waited ~2 s), the rest time out after 3 s of
// waiting.
