/**
 * Virtual waiting room for an on-sale: everyone gets a place in line, and users are let into the
 * booking flow at a rate the inventory service can handle. Admission expires so stalled users free
 * their slot.
 */
class WaitingRoom {
    private final int admitPerTick;
    private final long sessionMillis;
    private final Map<String, Long> seqOf = new HashMap<>(); // user → place in line
    private final Map<Long, Long> admittedAt = new HashMap<>(); // seq → when admitted
    // seq < admittedUpTo are admitted
    private long nextSeq = 0, admittedUpTo = 0;

    WaitingRoom(int admitPerTick, long sessionMillis) {
        this.admitPerTick = admitPerTick;
        this.sessionMillis = sessionMillis;
    }

    /** Idempotent: refreshing the page keeps your place. */
    synchronized long join(String userId) {
        return seqOf.computeIfAbsent(userId, u -> nextSeq++);
    }

    /** Called on a timer (e.g. every second): admit the next batch in arrival order. */
    synchronized void tick(long now) {
        long upTo = Math.min(nextSeq, admittedUpTo + admitPerTick);
        for (long s = admittedUpTo; s < upTo; s++) admittedAt.put(s, now);
        admittedUpTo = upTo;
    }

    /** People ahead of you, or 0 once admitted. */
    synchronized long position(String userId) {
        long seq = seqOf.get(userId);
        return Math.max(0, seq - admittedUpTo + 1);
    }

    /**
     * The booking service checks this (in production: a signed, expiring token instead of a
     * lookup).
     */
    synchronized boolean mayBook(String userId, long now) {
        Long seq = seqOf.get(userId);
        if (seq == null || seq >= admittedUpTo) return false;
        return now - admittedAt.get(seq) <= sessionMillis;
    }
}
