static class Counters {         // a and b sit in the same cache line
    volatile long a;
    volatile long b;
}

static class PaddedCounters {   // 120 bytes of padding push b onto another line
    volatile long a;
    long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
    volatile long b;
}

/** Two threads each bump their own counter. No data is shared, only a cache line. */
long millis(boolean padded) throws InterruptedException {
    long n = 200_000_000L;
    Counters same = new Counters();
    PaddedCounters apart = new PaddedCounters();
    Thread t1 = new Thread(() -> {
        for (long i = 0; i < n; i++) if (padded) apart.a++; else same.a++;
    });
    Thread t2 = new Thread(() -> {
        for (long i = 0; i < n; i++) if (padded) apart.b++; else same.b++;
    });
    long start = System.nanoTime();
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    return (System.nanoTime() - start) / 1_000_000;
}
