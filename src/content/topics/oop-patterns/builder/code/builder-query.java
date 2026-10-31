public final class SelectQuery {
    private final String sql;
    private final List<Object> params;
    private SelectQuery(String sql, List<Object> params) {
        this.sql = sql;
        this.params = List.copyOf(params);
    }
    public String sql() { return sql; }
    public List<Object> params() { return params; }

    public static Builder select(String... columns) { return new Builder(columns); }

    public static final class Builder {
        private final String[] columns;
        private String table;
        private final List<String> where = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();
        private String orderBy;
        private Integer limit;

        private Builder(String[] columns) {
            if (columns.length == 0) throw new IllegalArgumentException("select at least one column");
            this.columns = columns;
        }
        public Builder from(String table)                    { this.table = table; return this; }
        public Builder where(String condition, Object value) {
            where.add(condition);
            params.add(value);
            return this;
        }
        public Builder orderBy(String column)                { this.orderBy = column; return this; }
        public Builder limit(int n)                          { this.limit = n; return this; }

        public SelectQuery build() {
            if (table == null) throw new IllegalStateException("from() is required");
            var sb = new StringBuilder("SELECT ")
                    .append(String.join(", ", columns))
                    .append(" FROM ").append(table);
            // conditions use '?' placeholders
            if (!where.isEmpty()) sb.append(" WHERE ").append(String.join(" AND ", where));
            if (orderBy != null) sb.append(" ORDER BY ").append(orderBy);
            if (limit != null) sb.append(" LIMIT ").append(limit);
            return new SelectQuery(sb.toString(), params);
        }
    }
}

var q = SelectQuery.select("name", "age").from("users")
        .where("age > ?", 18).orderBy("name").limit(10)
        .build();
// q.sql()    → SELECT name, age FROM users WHERE age > ? ORDER BY name LIMIT 10
// q.params() → [18]   (bound by JDBC, never concatenated — no SQL injection)
