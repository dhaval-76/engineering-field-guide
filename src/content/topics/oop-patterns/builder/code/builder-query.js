export function select(...columns) {
  if (columns.length === 0) throw new Error('select at least one column');
  const q = { table: null, where: [], params: [], orderBy: null, limit: null };
  const builder = {
    from(table) { q.table = table; return builder; },
    where(condition, value) { q.where.push(condition); q.params.push(value); return builder; },
    orderBy(column) { q.orderBy = column; return builder; },
    limit(n) { q.limit = n; return builder; },
    build() {
      if (!q.table) throw new Error('from() is required');
      let sql = `SELECT ${columns.join(', ')} FROM ${q.table}`;
      // conditions use $n / ? placeholders
      if (q.where.length) sql += ` WHERE ${q.where.join(' AND ')}`;
      if (q.orderBy) sql += ` ORDER BY ${q.orderBy}`;
      if (q.limit !== null) sql += ` LIMIT ${Number(q.limit)}`;
      return Object.freeze({ sql, params: Object.freeze([...q.params]) });
    },
  };
  return builder;
}

const q = select('name', 'age').from('users').where('age > ?', 18).orderBy('name').limit(10).build();
// q.sql    → SELECT name, age FROM users WHERE age > ? ORDER BY name LIMIT 10
// q.params → [18]   (bound by the driver, never concatenated — no SQL injection)
