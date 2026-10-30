// npm i lru-cache — the widely used Node implementation.
import { LRUCache } from 'lru-cache';

const profiles = new LRUCache({
  max: 10_000,
  ttl: 30 * 60 * 1000,
  // fetchMethod de-duplicates concurrent misses for the same key.
  fetchMethod: async (userId) => userRepository.load(userId),
});

const profile = await profiles.fetch(userId);
