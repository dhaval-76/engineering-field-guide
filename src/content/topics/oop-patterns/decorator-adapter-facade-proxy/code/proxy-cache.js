// npm i lru-cache
import { LRUCache } from 'lru-cache';

// Proxy: same interface as the real catalog ({ byId(id) }), controls ACCESS to it — here, caching.
export function cachingCatalog(real) {
  const cache = new LRUCache({ max: 10_000, ttl: 5 * 60 * 1000, fetchMethod: (id) => real.byId(id) });
  return { byId: (id) => cache.fetch(id) }; // concurrent misses for one id share a single load
}

// Other proxy flavours: access control (check permissions before delegating), lazy loading,
// remote proxies (an HTTP/gRPC client stub), and JavaScript's built-in Proxy for intercepting property access.
