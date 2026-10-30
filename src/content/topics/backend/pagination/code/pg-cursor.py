def encode(row) -> str:
    return base64.urlsafe_b64encode(json.dumps([row["created_at"].isoformat(), row["id"]]).encode()).decode()

def decode(cursor: str) -> tuple[datetime, str]:
    t, i = json.loads(base64.urlsafe_b64decode(cursor))
    return datetime.fromisoformat(t), i

@app.get("/v1/customers/{customer_id}/orders")
def list_orders(customer_id: str, cursor: str | None = None, limit: int = Query(20, le=100)):
    sql = "SELECT id, created_at, total FROM orders WHERE customer_id = %s"
    args: list = [customer_id]
    if cursor:
        sql += " AND (created_at, id) < (%s, %s)"
        args += list(decode(cursor))
    sql += " ORDER BY created_at DESC, id DESC LIMIT %s"
    rows = db.execute(sql, [*args, limit + 1]).fetchall()
    page = rows[:limit]
    return {"data": page, "nextCursor": encode(page[-1]) if len(rows) > limit else None}
