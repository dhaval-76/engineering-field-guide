cache = LRUCache(2)
cache.put(1, 1)
cache.put(2, 2)
assert cache.get(1) == 1        # 1 becomes most recent; order is now [1, 2]
cache.put(3, 3)                 # full → evicts 2 (least recent)
assert cache.get(2) is None
cache.put(1, 10)                # update keeps size at 2, refreshes 1
cache.put(4, 4)                 # evicts 3
assert cache.get(3) is None and cache.get(1) == 10 and cache.get(4) == 4
