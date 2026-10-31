// synchronized: mutual exclusion + visibility. Reentrant: the same thread may re-enter.
class Inventory {
    private final Map<String, Integer> stock = new HashMap<>();
    synchronized boolean reserve(String sku, int qty) {        // locks on `this`
        int have = stock.getOrDefault(sku, 0);
        if (have < qty) return false;
        stock.put(sku, have - qty);
        return true;
    }
}

// ReentrantLock: the same guarantees plus tryLock (with timeout), interruptible waits,
// optional fairness, and multiple conditions.
class Account {
    private final ReentrantLock lock = new ReentrantLock();
    private long balance;
    boolean tryWithdraw(long amt) throws InterruptedException {
        // don't wait forever
        if (!lock.tryLock(200, TimeUnit.MILLISECONDS)) return false;
        try {
            if (balance < amt) return false;
            balance -= amt;
            return true;
        } finally {
            lock.unlock();                                    // ALWAYS in finally
        }
    }
}

// ReadWriteLock: many concurrent readers OR one writer — for read-mostly data.
class Catalogue {
    private final ReadWriteLock rw = new ReentrantReadWriteLock();
    private final Map<String, Product> products = new HashMap<>();
    Product get(String id) {
        rw.readLock().lock();
        try { return products.get(id); } finally { rw.readLock().unlock(); }
    }
    void put(Product p) {
        rw.writeLock().lock();
        try { products.put(p.id(), p); } finally { rw.writeLock().unlock(); }
    }
}
// StampedLock adds optimistic reads (validate afterwards) for very read-heavy hot paths.
