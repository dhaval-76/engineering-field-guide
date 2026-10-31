@Component
class TransactionRetrier {
    // serialization failure, deadlock
    private static final Set<String> RETRYABLE = Set.of("40001", "40P01");
    private final TransactionTemplate tx;

    TransactionRetrier(PlatformTransactionManager tm) {
        this.tx = new TransactionTemplate(tm);
        this.tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    /**
     * Runs the WHOLE unit of work again on a retryable failure — every read is redone.
     */
    public <T> T run(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return tx.execute(status -> work.get());
            } catch (DataAccessException e) {
                if (attempt >= 5 || !isRetryable(e)) throw e;
                sleepWithJitter(attempt);
            }
        }
    }

    private static boolean isRetryable(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause())
            if (t instanceof SQLException s && RETRYABLE.contains(s.getSQLState())) return true;
        return false;
    }

    private static void sleepWithJitter(int attempt) {
        long max = Math.min(1000, 20L << attempt);            // 40, 80, 160 … ms, capped
        try { Thread.sleep(ThreadLocalRandom.current().nextLong(max)); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new IllegalStateException(ie); }
    }
}

// Usage: the lambda must not have side effects outside the database (no emails, no HTTP).
retrier.run(() -> { scheduling.takeOffCall("alice", 1); return null; });
