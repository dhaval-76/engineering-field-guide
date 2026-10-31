interface Balancer { String pick(); default void done(String server) {} }

/** Plain round robin: equal turns, blind to load. */
class RoundRobin implements Balancer {
    private final List<String> servers; private final AtomicInteger next = new AtomicInteger();
    RoundRobin(List<String> servers) { this.servers = List.copyOf(servers); }
    public String pick() { return servers.get(Math.floorMod(next.getAndIncrement(), servers.size())); }
}

/** Smooth weighted round robin (nginx): weights 5,1,1 → a a b a c a a — spread out, not bunched. */
class SmoothWeighted implements Balancer {
    private final Map<String, Integer> weight; private final Map<String, Integer> current = new LinkedHashMap<>();
    SmoothWeighted(Map<String, Integer> weights) {
        this.weight = new LinkedHashMap<>(weights);
        weights.keySet().forEach(s -> current.put(s, 0));
    }
    public synchronized String pick() {
        int total = 0; String best = null;
        for (var e : weight.entrySet()) {
            current.merge(e.getKey(), e.getValue(), Integer::sum);       // everyone gains its weight
            total += e.getValue();
            if (best == null || current.get(e.getKey()) > current.get(best)) best = e.getKey();
        }
        current.merge(best, -total, Integer::sum);                       // winner pays the total
        return best;
    }
}

/** Least connections: the server with the fewest requests in flight. */
class LeastConnections implements Balancer {
    final Map<String, Integer> active = new ConcurrentHashMap<>();
    LeastConnections(List<String> servers) { servers.forEach(s -> active.put(s, 0)); }
    public synchronized String pick() {
        String s = Collections.min(active.entrySet(), Map.Entry.comparingByValue()).getKey();
        active.merge(s, 1, Integer::sum);
        return s;
    }
    public synchronized void done(String server) { active.merge(server, -1, Integer::sum); }
}

/** Power of two choices: sample two at random, take the less loaded. Nearly as good, no global scan. */
class PowerOfTwo implements Balancer {
    final Map<String, Integer> active = new ConcurrentHashMap<>();
    private final List<String> servers; private final Random random;
    PowerOfTwo(List<String> servers, Random random) {
        this.servers = List.copyOf(servers); this.random = random;
        servers.forEach(s -> active.put(s, 0));
    }
    public synchronized String pick() {
        String a = servers.get(random.nextInt(servers.size())), b = servers.get(random.nextInt(servers.size()));
        String s = active.get(a) <= active.get(b) ? a : b;
        active.merge(s, 1, Integer::sum);
        return s;
    }
    public synchronized void done(String server) { active.merge(server, -1, Integer::sum); }
}
