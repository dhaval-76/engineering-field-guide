// Dependencies: org.hibernate.orm:hibernate-jcache + a JCache provider (Ehcache 3,
// Caffeine's jcache)
//
// application.yml
//   spring.jpa.properties.hibernate.cache.use_second_level_cache: true
//   spring.jpa.properties.hibernate.cache.region.factory_class: jcache
//   spring.jpa.properties.jakarta.persistence.sharedCache.mode: ENABLE_SELECTIVE

@Entity
@Cacheable                                                  // opt this entity in
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
// small, read constantly, never updated by the app
public class Country {
    @Id private String code;
    private String name;
    private String currency;
}

// Strategies:
//   READ_ONLY            immutable data — fastest; updating the entity throws
//   NONSTRICT_READ_WRITE rare updates; a brief window of stale reads is acceptable
// READ_WRITE           soft-locks entries during updates — no stale reads within this JVM
//   TRANSACTIONAL        needs a JTA-capable cache provider
