# Redis Cluster: 16,384 hash slots; slot = CRC16(key) % 16384; each primary owns slot
# ranges.
redis-cli -c -h cache-0 CLUSTER KEYSLOT user:42            # → 15880
redis-cli -c -h cache-0 CLUSTER KEYSLOT foo                # → 12182

# Hash tags: only the part inside {…} is hashed, so related keys share a slot
# and multi-key commands / transactions work on them.
redis-cli -c -h cache-0 CLUSTER KEYSLOT "{user:42}:profile"  # → 15880
redis-cli -c -h cache-0 CLUSTER KEYSLOT "{user:42}:cart"     # → 15880
redis-cli -c -h cache-0 MGET "{user:42}:profile" "{user:42}:cart"

# A client that asks the wrong node is redirected; smart clients cache the slot map and
# refresh on MOVED.
#   GET user:42  →  -MOVED 15880 10.0.3.12:6379
# slot ranges → primary + replicas
redis-cli -h cache-0 CLUSTER SHARDS

# Memory policy for a cache (not a database): evict approximately least-recently-used keys
# at the limit
redis-cli -h cache-0 CONFIG SET maxmemory 24gb
# or allkeys-lfu for stable popularity
redis-cli -h cache-0 CONFIG SET maxmemory-policy allkeys-lru
# more samples = closer to true LRU
redis-cli -h cache-0 CONFIG SET maxmemory-samples 10

# Find hot and big keys before they hurt
redis-cli -h cache-0 --hotkeys        # needs an LFU policy
redis-cli -h cache-0 --bigkeys
