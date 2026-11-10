import sqlite3


def find_user_unsafe(db: sqlite3.Connection, email: str) -> str | None:
    """UNSAFE: the email becomes part of the SQL text, so it can change the query itself."""
    row = db.execute(f"SELECT name FROM users WHERE email = '{email}'").fetchone()
    return row[0] if row else None


def find_user(db: sqlite3.Connection, email: str) -> str | None:
    """Safe: the SQL is fixed and the email is sent separately, as data only."""
    row = db.execute("SELECT name FROM users WHERE email = ?", (email,)).fetchone()
    return row[0] if row else None


# Column names can't be parameters, so map user choices to fixed, known names.
SORTABLE = {"date": "created_at", "total": "amount"}


def order_ids(db: sqlite3.Connection, user_id: int, sort: str, descending: bool) -> list[int]:
    column = SORTABLE.get(sort)
    if column is None:
        raise ValueError(f"cannot sort by {sort}")
    direction = "DESC" if descending else "ASC"
    sql = f"SELECT id FROM orders WHERE user_id = ? ORDER BY {column} {direction}"
    return [row[0] for row in db.execute(sql, (user_id,))]
