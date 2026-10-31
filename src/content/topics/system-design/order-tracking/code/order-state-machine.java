enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURNED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT =
            Map.of(
                    PLACED, EnumSet.of(CONFIRMED, CANCELLED),
                    CONFIRMED, EnumSet.of(PREPARING, CANCELLED),
                    PREPARING, EnumSet.of(OUT_FOR_DELIVERY, CANCELLED),
                    OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
                    DELIVERED, EnumSet.of(RETURNED),
                    CANCELLED, EnumSet.noneOf(OrderStatus.class),
                    RETURNED, EnumSet.noneOf(OrderStatus.class));

    boolean canMoveTo(OrderStatus next) {
        return NEXT.get(this).contains(next);
    }
}

@Service
class OrderTracking {
    private final JdbcTemplate jdbc;

    /** Applies a partner event exactly once and only if it's a valid next step. */
    @Transactional
    public boolean apply(long orderId, String eventId, OrderStatus next, String actor) {
        if (jdbc.update(
                        "INSERT INTO processed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING",
                        eventId)
                == 0) return false; // duplicate delivery

        OrderStatus current =
                OrderStatus.valueOf(
                        jdbc.queryForObject(
                                "SELECT status FROM orders WHERE id = ?", String.class, orderId));
        // late or invalid event: ignore (and log)
        if (!current.canMoveTo(next)) return false;

        // Conditional update: if another actor changed the status meanwhile, nothing
        // happens.
        int moved =
                jdbc.update(
                        "UPDATE orders SET status = ?, updated_at = now() WHERE id = ? AND status ="
                            + " ?",
                        next.name(),
                        orderId,
                        current.name());
        // retried by the caller
        if (moved == 0) throw new ConcurrentStatusChangeException(orderId);

        jdbc.update(
                "INSERT INTO order_status_history (order_id, status, actor, at) VALUES (?, ?, ?,"
                    + " now())",
                orderId,
                next.name(),
                actor);
        jdbc.update(
                "INSERT INTO outbox (id, type, aggregate_id, payload) VALUES (gen_random_uuid(),"
                    + " 'OrderStatusChanged', ?, ?::jsonb)",
                String.valueOf(orderId),
                "{\"orderId\":%d,\"status\":\"%s\"}".formatted(orderId, next));
        return true;
    }
}
