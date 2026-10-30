// Expecting ~10,000 entries? The table must hold them below the 0.75 load factor,
// otherwise it resizes 10 times on the way up (16 → 32 → … → 16,384), rehashing everything each time.

Map<String, User> bad  = new HashMap<>();              // starts at 16 buckets
Map<String, User> ok   = new HashMap<>(13_334);        // capacity = expected / 0.75, rounded up to 16,384
Map<String, User> best = HashMap.newHashMap(10_000);    // Java 19+: does that arithmetic for you

// Frequency counting: one atomic, readable call instead of get + put.
Map<String, Integer> counts = new HashMap<>();
for (String w : words) counts.merge(w, 1, Integer::sum);
