public final class Library {
    // bookId → userId
    private final ConcurrentHashMap<String, String> checkedOut = new ConcurrentHashMap<>();

    /** At most one user can hold a book; different books never block each other. */
    public boolean checkout(String userId, String bookId) {
        // atomic per key, no explicit lock
        return checkedOut.putIfAbsent(bookId, userId) == null;
    }

    public boolean returnBook(String userId, String bookId) {
        // only the holder can return it
        return checkedOut.remove(bookId, userId);
    }

    /**
     * Swap two books between users atomically: lock in a global order to avoid deadlock.
     */
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void swap(String bookA, String bookB) {
        String first = bookA.compareTo(bookB) < 0 ? bookA : bookB;
        String second = first.equals(bookA) ? bookB : bookA;
        ReentrantLock l1 = locks.computeIfAbsent(first, k -> new ReentrantLock());
        ReentrantLock l2 = locks.computeIfAbsent(second, k -> new ReentrantLock());
        l1.lock();
        try {
            l2.lock();
            try {
                String ua = checkedOut.get(bookA), ub = checkedOut.get(bookB);
                if (ua != null && ub != null) { checkedOut.put(bookA, ub); checkedOut.put(bookB, ua); }
            } finally { l2.unlock(); }
        } finally { l1.unlock(); }
    }
}
// In a real multi-instance service the source of truth is the database:
// UPDATE books SET holder = ? WHERE id = ? AND holder IS NULL  → 1 row = success.
