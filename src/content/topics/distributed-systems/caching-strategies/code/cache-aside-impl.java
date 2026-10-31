@Service
class ProductQueries {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redis;
    private final ProductRepository repo;
    private final ObjectMapper json;

    ProductView get(long id) {
        // prefix + schema version in the key
        String key = "product:v1:" + id;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return read(cached);         // hit
        } catch (RedisConnectionFailureException e) {
            // a cache outage must not be an outage
            log.warn("cache unavailable, reading from DB", e);
            return load(id);
        }
        ProductView v = load(id);                            // miss → database
        redis.opsForValue().set(key, write(v), jitter(TTL)); // fill with a jittered TTL
        return v;
    }

    private ProductView load(long id) {
        return repo.findView(id).orElseThrow(() -> new NotFoundException("product", id));
    }

    // ±10% so keys don't expire together
    static Duration jitter(Duration base) {
        long ms = base.toMillis();
        return Duration.ofMillis(ms + ThreadLocalRandom.current().nextLong(-ms / 10, ms / 10));
    }
}

@Service
class ProductCommands {
    @Transactional
    public void changePrice(long id, BigDecimal price) {
        repo.findById(id).orElseThrow().setPrice(price);
        // delivered after commit (below)
        events.publishEvent(new ProductChanged(id));
    }
}

@Component
class ProductCacheInvalidator {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ProductChanged e) {
        redis.delete("product:v1:" + e.id());                // delete, don't overwrite
    }
}
