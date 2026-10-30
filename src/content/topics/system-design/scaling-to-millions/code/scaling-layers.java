@Service
class ProductService {
    // Level 1: in-process cache — microseconds, per instance, short TTL limits staleness.
    private final Cache<Long, ProductView> local = Caffeine.newBuilder()
        .maximumSize(50_000).expireAfterWrite(Duration.ofSeconds(30)).recordStats().build();

    private final ProductRepository repo;
    private final RedisTemplate<String, ProductView> redis;   // Level 2: shared across instances

    ProductView get(long id) {
        return local.get(id, k -> {
            ProductView v = redis.opsForValue().get("product:v1:" + k);
            if (v == null) {
                v = repo.findView(k).orElseThrow(() -> new NotFoundException("product", k));
                redis.opsForValue().set("product:v1:" + k, v, Duration.ofMinutes(10));
            }
            return v;
        });
    }
}

@RestController
class OrderController {
    private final OrderService orders;
    private final RateLimiter perClient;      // e.g. Bucket4j/Redis: 20 req/s per API key

    @PostMapping("/orders")
    ResponseEntity<OrderView> place(@RequestHeader("X-Api-Key") String key, @RequestBody PlaceOrder cmd) {
        if (!perClient.tryAcquire(key)) {
            return ResponseEntity.status(429).header("Retry-After", "1").build();   // shed early
        }
        OrderView placed = orders.place(cmd);  // fast path: DB transaction + outbox row only
        return ResponseEntity.status(201).body(placed);
    }
}

// Slow work leaves the request path: a worker consumes the outbox event.
@Component
class OrderSideEffects {
    @KafkaListener(topics = "orders", groupId = "emails")
    void onPlaced(OrderPlaced e) {
        emails.sendConfirmation(e);           // retried independently; never slows checkout
    }
}
