// Every driver works the same way; node:sqlite is used here because it is built in.

/** UNSAFE: the email becomes part of the SQL text, so it can change the query itself. */
function findUserUnsafe(db, email) {
  return db.prepare(`SELECT name FROM users WHERE email = '${email}'`).get()?.name ?? null;
}

/** Safe: the SQL is fixed and the email is sent separately, as data only. */
function findUser(db, email) {
  return db.prepare('SELECT name FROM users WHERE email = ?').get(email)?.name ?? null;
}

/** Column names can't be parameters, so map user choices to fixed, known names. */
const SORTABLE = { date: 'created_at', total: 'amount' };

function orderIds(db, userId, sort, descending) {
  const column = Object.hasOwn(SORTABLE, sort) ? SORTABLE[sort] : null;
  if (!column) throw new Error(`cannot sort by ${sort}`);
  const direction = descending ? 'DESC' : 'ASC';
  const sql = `SELECT id FROM orders WHERE user_id = ? ORDER BY ${column} ${direction}`;
  return db.prepare(sql).all(userId).map((row) => row.id);
}
