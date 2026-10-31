@Service
class SessionStore {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    SessionStore(StringRedisTemplate redis, ObjectMapper json) { this.redis = redis; this.json = json; }

    String create(SessionData data) throws JsonProcessingException {
        byte[] bytes = new byte[32];
        // unguessable id, not a UUID v4 from Math.random
        new SecureRandom().nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // SET … EX 1800
        redis.opsForValue().set(key(id), json.writeValueAsString(data), TTL);
        return id;
    }

    /** Reads and slides the expiry in ONE atomic command (GETEX, Redis 6.2+). */
    Optional<SessionData> touch(String id) throws JsonProcessingException {
        String raw = redis.opsForValue().getAndExpire(key(id), TTL);
        return raw == null ? Optional.empty() : Optional.of(json.readValue(raw, SessionData.class));
    }

    void invalidate(String id) { redis.delete(key(id)); }            // logout

    private static String key(String id) { return "session:" + id; }
}
// Spring Session (spring-session-data-redis) does all of this for HttpSession
// transparently.
