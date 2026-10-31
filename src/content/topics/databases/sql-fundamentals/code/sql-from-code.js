// node-postgres (pg): $1, $2 … placeholders
import pg from 'pg';
const pool = new pg.Pool();

export async function placeOrder(customerId, productId, qty) {
  const client = await pool.connect(); // a transaction needs ONE connection
  try {
    await client.query('BEGIN');
    const reserved = await client.query(
      'UPDATE products SET stock = stock - $1 WHERE id = $2 AND stock >= $1',
      [qty, productId],
    );
    if (reserved.rowCount === 0) throw new OutOfStockError(productId);

    const { rows } = await client.query(
      'INSERT INTO orders (customer_id, total) SELECT $1, price * $2 FROM products WHERE id = $3 RETURNING id',
      [customerId, qty, productId],
    );
    await client.query('COMMIT');
    return rows[0].id;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release(); // always give the connection back
  }
}

// ✗ client.query(`SELECT * FROM customers WHERE email = '${email}'`)  — SQL injection
