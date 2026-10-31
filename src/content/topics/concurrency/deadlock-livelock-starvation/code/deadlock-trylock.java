// Break "no preemption": if you can't get the second lock soon, give up the first and
// retry.
boolean transfer(Account from, Account to, BigDecimal amount) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
    while (System.nanoTime() < deadline) {
        if (from.lock().tryLock(50, MILLISECONDS)) {
            try {
                if (to.lock().tryLock(50, MILLISECONDS)) {
                    try {
                        from.debit(amount);
                        to.credit(amount);
                        return true;
                    } finally {
                        to.lock().unlock();
                    }
                }
            } finally {
                from.lock().unlock();
            }
        }
        // RANDOM backoff — avoids livelock
        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 20));
    }
    return false;   // caller reports "busy, try again"
}
