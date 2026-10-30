import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

// Caffeine: thread-safe, near lock-free reads, W-TinyLFU eviction, TTL support.
Cache<String, UserProfile> profiles = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .recordStats()
        .build();

// Only one thread loads a missing key; concurrent callers wait for that result
// instead of all hitting the database (prevents a cache stampede).
UserProfile p = profiles.get(userId, id -> userRepository.load(id));
