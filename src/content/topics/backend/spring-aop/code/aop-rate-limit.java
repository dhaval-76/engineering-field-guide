@Target(METHOD) @Retention(RUNTIME)
public @interface RateLimited { int perMinute(); }

@Aspect
@Component
class RateLimitAspect {
    // One token bucket per (caller, method); Caffeine evicts idle buckets so the map can't grow forever
    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10)).build();

    @Before("@annotation(limit)")
    void check(JoinPoint jp, RateLimited limit) {
        String caller = SecurityContextHolder.getContext().getAuthentication().getName();
        String key = caller + ":" + jp.getSignature().toShortString();
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket(limit.perMinute(), limit.perMinute() / 60.0));
        if (!bucket.tryConsume()) throw new RateLimitExceededException(limit.perMinute());   // → 429 via @ControllerAdvice
    }
}
// TokenBucket: see the rate-limiter design. This limits per instance only —
// with several instances, keep the buckets in Redis (see the distributed rate limiter).
