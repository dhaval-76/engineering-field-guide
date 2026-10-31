record Message(long offset, String key, String payload) {}

/** In-memory log-based pub/sub: each topic is an append-only log; each consumer group has its own offset. */
class Broker {
    private static final class Topic {
        final List<Message> log = new ArrayList<>();
        long baseOffset = 0;                                   // offset of log.get(0) after retention trims
        final Map<String, Long> committed = new HashMap<>();   // group → next offset to read
    }

    private final Map<String, Topic> topics = new ConcurrentHashMap<>();

    private Topic topic(String name) { return topics.computeIfAbsent(name, n -> new Topic()); }

    long publish(String topicName, String key, String payload) {
        Topic t = topic(topicName);
        synchronized (t) {
            long offset = t.baseOffset + t.log.size();
            t.log.add(new Message(offset, key, payload));
            t.notifyAll();                                     // wake long-polling consumers
            return offset;
        }
    }

    /** A new group starts at the end (only new messages) or the beginning (replay what's retained). */
    void subscribe(String topicName, String group, boolean fromBeginning) {
        Topic t = topic(topicName);
        synchronized (t) { t.committed.putIfAbsent(group, fromBeginning ? t.baseOffset : t.baseOffset + t.log.size()); }
    }

    /** Returns up to max messages after the group's committed offset. Nothing moves until commit. */
    List<Message> poll(String topicName, String group, int max) {
        Topic t = topic(topicName);
        synchronized (t) {
            Long from = t.committed.get(group);
            if (from == null) throw new IllegalStateException("not subscribed: " + group);
            int start = (int) (from - t.baseOffset);
            return List.copyOf(t.log.subList(start, Math.min(t.log.size(), start + max)));
        }
    }

    /** Acknowledge everything below nextOffset. A consumer that crashes before committing gets the batch again. */
    void commit(String topicName, String group, long nextOffset) {
        Topic t = topic(topicName);
        synchronized (t) { t.committed.merge(group, nextOffset, Math::max); }   // never move backwards by accident
    }

    /** Retention: drop messages every group has consumed (a real broker also trims by age/size). */
    int trim(String topicName) {
        Topic t = topic(topicName);
        synchronized (t) {
            long minCommitted = t.committed.values().stream().mapToLong(Long::longValue)
                    .min().orElse(t.baseOffset + t.log.size());
            int drop = (int) (minCommitted - t.baseOffset);
            t.log.subList(0, drop).clear();
            t.baseOffset = minCommitted;
            return drop;
        }
    }
}
