class UnsafeCounter {
    private int count;
    void increment() { count++; }          // read → add → write: three steps, not one
    int get() { return count; }
}

var counter = new UnsafeCounter();
try (var pool = Executors.newFixedThreadPool(8)) {
    for (int i = 0; i < 100_000; i++) pool.execute(counter::increment);
}
// usually LESS than 100000 — updates were lost
System.out.println(counter.get());

// Check-then-act is the same bug at a larger scale:
// two threads can both see "absent" and both load
if (!map.containsKey(key)) map.put(key, load(key));
map.computeIfAbsent(key, this::load);                 // ✓ atomic on ConcurrentHashMap
