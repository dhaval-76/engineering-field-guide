// build: spring-boot-starter-cache, caffeine (local) and/or
// spring-boot-starter-data-redis
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer redisCaches() {
        return builder -> builder
            .withCacheConfiguration("products", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())));
    }
}
// Local-only alternative (application.yml):
//   spring.cache.type: caffeine
//   spring.cache.caffeine.spec: maximumSize=10000,expireAfterWrite=30s

@Service
class ProductQueries {
    // Cache-aside via proxy. sync = true: one loader per key per instance (stampede
    // protection).
    @Cacheable(cacheNames = "products", key = "#id", sync = true)
    public ProductView get(long id) {
        return repo.findView(id).orElseThrow(() -> new NotFoundException("product", id));
    }
}

@Service
class ProductCommands {
    @Transactional
    public void changePrice(long id, BigDecimal price) {
        repo.findById(id).orElseThrow().setPrice(price);
        events.publishEvent(new ProductChanged(id));
    }
    // ✗ @CacheEvict here can evict BEFORE commit: a concurrent reader could reload the
    // old
    //   row into the cache and keep it until the TTL expires.
}

@Component
class ProductCacheEvictor {
    private final CacheManager caches;
    ProductCacheEvictor(CacheManager caches) { this.caches = caches; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ProductChanged e) {
        caches.getCache("products").evict(e.id());
    }
}
