// LRUCache from the from-scratch implementation above
import assert from 'node:assert/strict';

const cache = new LRUCache(2);
cache.put(1, 1);
cache.put(2, 2);
assert.equal(cache.get(1), 1);       // 1 becomes most recent; order is now [1, 2]
cache.put(3, 3);                     // full → evicts 2 (least recent)
assert.equal(cache.get(2), undefined);
cache.put(1, 10);                    // update keeps size at 2, refreshes 1
cache.put(4, 4);                     // evicts 3
assert.equal(cache.get(3), undefined);
assert.equal(cache.get(1), 10);
assert.equal(cache.get(4), 4);
