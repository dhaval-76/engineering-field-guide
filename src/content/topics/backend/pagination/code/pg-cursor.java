public record Page<T>(List<T> data, String nextCursor) {}

/** Opaque cursor: clients pass it back unchanged and must not parse it. */
record Cursor(Instant createdAt, String id) {
    String encode() {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((createdAt.toEpochMilli() + ":" + id).getBytes(UTF_8));
    }
    static Cursor decode(String s) {
        String[] p = new String(Base64.getUrlDecoder().decode(s), UTF_8).split(":", 2);
        return new Cursor(Instant.ofEpochMilli(Long.parseLong(p[0])), p[1]);
    }
}

@GetMapping("/v1/customers/{customerId}/orders")
Page<OrderView> list(@PathVariable String customerId,
                     @RequestParam(required = false) String cursor,
                     @RequestParam(defaultValue = "20") @Max(100) int limit) {
    List<OrderView> rows = (cursor == null)
        ? repo.firstPage(customerId, limit + 1)
        : repo.pageAfter(customerId, Cursor.decode(cursor), limit + 1);

    boolean hasMore = rows.size() > limit;
    List<OrderView> page = hasMore ? rows.subList(0, limit) : rows;
    OrderView last = page.isEmpty() ? null : page.get(page.size() - 1);
    return new Page<>(page, hasMore ? new Cursor(last.createdAt(), last.id()).encode() : null);
}

// Repository (JdbcTemplate / jOOQ / native query — JPQL has no LIMIT clause)
List<OrderView> pageAfter(String customerId, Cursor c, int n) {
    return jdbc.query("""
        SELECT id, created_at, total FROM orders
        WHERE customer_id = ? AND (created_at, id) < (?, ?)
        ORDER BY created_at DESC, id DESC
        LIMIT ?""", ORDER_VIEW, customerId, Timestamp.from(c.createdAt()), c.id(), n);
}
