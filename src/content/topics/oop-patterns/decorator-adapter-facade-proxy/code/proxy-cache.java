public interface ProductCatalog { Product byId(String id); }

final class DbProductCatalog implements ProductCatalog {
    public Product byId(String id) { /* SELECT … */ return null; }
}

// Proxy: same interface, controls ACCESS to the real object (here: caching).
final class CachingCatalog implements ProductCatalog {
    private final ProductCatalog real;
    private final Cache<String, Product> cache = Caffeine.newBuilder().maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5)).build();

    CachingCatalog(ProductCatalog real) { this.real = real; }

    public Product byId(String id) { return cache.get(id, real::byId); }
}

// Other proxy flavours: access control (check permissions before delegating),
// lazy loading (Hibernate entity proxies), remote proxy (gRPC/REST client stub),
// and Spring AOP proxies that add @Transactional / @Cacheable around your beans.
