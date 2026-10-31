// 1. Mutual exclusion — one thread in the critical section at a time
class SyncCounter {
    private long count;
    synchronized void increment() { count++; }
    synchronized long get() { return count; }
}

// 2. Compare-and-swap — lock-free; retries if another thread changed the value first
class AtomicCounter {
    private final AtomicLong count = new AtomicLong();
    void increment() { count.incrementAndGet(); }
    long get() { return count.get(); }
}

// 3. Striped counter — spreads contention across cells; sum() adds them up
class AdderCounter {
    private final LongAdder count = new LongAdder();
    void increment() { count.increment(); }
    // not an atomic snapshot while writers are active
    long get() { return count.sum(); }
}

// What CAS does in one CPU instruction:  if (value == expected) { value = next; return
// true; } else return false;
