ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

// ✗ get-then-put: two threads read 5 and both write 6
int v = counts.getOrDefault(word, 0);
counts.put(word, v + 1);

// ✓ Atomic per-key operations (the function runs while that key's bin is locked)
counts.merge(word, 1, Integer::sum);                            // add or insert
counts.compute(word, (k, old) -> old == null ? 1 : old + 1);     // full control
counts.computeIfAbsent(userId, id -> loadProfile(id));           // load once per key
// returns the existing value if someone won
counts.putIfAbsent(sessionId, token);
counts.replace(key, expectedOld, newValue);                      // CAS on a value
// remove only if unchanged
counts.remove(key, expectedValue);

// Hot counters: one LongAdder per key avoids contention on the same bin
ConcurrentHashMap<String, LongAdder> hits = new ConcurrentHashMap<>();
hits.computeIfAbsent(endpoint, k -> new LongAdder()).increment();

// Keep the lambdas in compute/computeIfAbsent SHORT and side-effect free:
// they run while holding a lock on that bin, other writers to it wait, and the function
// must not modify the same map (that can deadlock or throw IllegalStateException).
