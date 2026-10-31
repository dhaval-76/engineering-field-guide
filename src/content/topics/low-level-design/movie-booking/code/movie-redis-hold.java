// Redis variant for very hot shows: one key per held seat, auto-expiring.
boolean holdSeat(String showId, String seatId, String holdId) {
    // SET key value NX PX ttl — succeeds only if nobody holds the seat; expires on its
    // own
    return Boolean.TRUE.equals(
            redis.opsForValue()
                    .setIfAbsent(
                            "hold:" + showId + ":" + seatId, holdId, Duration.ofMinutes(8)));
}

// Release only if WE still hold it (compare-and-delete must be atomic → Lua):
private static final String RELEASE_IF_OWNER =
        "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else"
            + " return 0 end";

void releaseSeat(String showId, String seatId, String holdId) {
    redis.execute(
            new DefaultRedisScript<>(RELEASE_IF_OWNER, Long.class),
            List.of("hold:" + showId + ":" + seatId),
            holdId);
}
// The database row is still the source of truth for BOOKED; Redis only arbitrates
// short-lived holds.

