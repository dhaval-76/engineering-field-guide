// Iteration is weakly consistent: never throws ConcurrentModificationException,
// reflects the map at some point(s) during iteration, may or may not include concurrent changes.
for (var e : counts.entrySet()) report(e.getKey(), e.getValue());

// Need a consistent snapshot? Copy first (still not atomic with respect to writers — quiesce if exact).
Map<String, Integer> snapshot = Map.copyOf(counts);

// size() is an estimate under concurrent updates; mappingCount() returns a long for huge maps.
long n = counts.mappingCount();

// Bulk operations run in parallel above a threshold:
long total = counts.reduceValuesToLong(1_000, Integer::longValue, 0L, Long::sum);
String hot = counts.search(1_000, (k, v) -> v > 10_000 ? k : null);
