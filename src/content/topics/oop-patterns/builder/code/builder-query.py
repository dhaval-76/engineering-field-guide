from dataclasses import dataclass

@dataclass(frozen=True)
class SelectQuery:
    sql: str
    params: tuple

class _Builder:
    def __init__(self, columns: tuple[str, ...]):
        if not columns:
            raise ValueError("select at least one column")
        self._columns, self._table, self._where, self._params = columns, None, [], []
        self._order_by, self._limit = None, None

    def from_(self, table: str) -> "_Builder":          # `from` is a keyword in Python
        self._table = table
        return self

    def where(self, condition: str, value) -> "_Builder":
        self._where.append(condition)
        self._params.append(value)
        return self

    def order_by(self, column: str) -> "_Builder":
        self._order_by = column
        return self

    def limit(self, n: int) -> "_Builder":
        self._limit = int(n)
        return self

    def build(self) -> SelectQuery:
        if self._table is None:
            raise ValueError("from_() is required")
        sql = f"SELECT {', '.join(self._columns)} FROM {self._table}"
        if self._where:
            sql += " WHERE " + " AND ".join(self._where)  # conditions use placeholders
        if self._order_by:
            sql += f" ORDER BY {self._order_by}"
        if self._limit is not None:
            sql += f" LIMIT {self._limit}"
        return SelectQuery(sql, tuple(self._params))

def select(*columns: str) -> _Builder:
    return _Builder(columns)

q = select("name", "age").from_("users").where("age > %s", 18).order_by("name").limit(10).build()
# q.sql    → SELECT name, age FROM users WHERE age > %s ORDER BY name LIMIT 10
# q.params → (18,)   (bound by the driver, never concatenated — no SQL injection)
