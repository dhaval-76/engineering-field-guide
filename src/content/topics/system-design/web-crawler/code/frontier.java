/** Per-host FIFO queues plus a heap of (next time this host may be fetched, host). */
class PoliteFrontier {
    private static final Set<String> TRACKING = Set.of("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "gclid", "fbclid");
    private record Ready(double at, String host) {}

    private final double defaultDelay;
    private final Map<String, Deque<String>> queues = new HashMap<>();
    // robots.txt Crawl-delay or adaptive
    final Map<String, Double> delay = new HashMap<>();
    private final Map<String, Double> nextAllowed = new HashMap<>();
    private final PriorityQueue<Ready> ready = new PriorityQueue<>(Comparator.comparingDouble(Ready::at).thenComparing(Ready::host));
    // a Bloom filter at web scale
    final Set<String> seen = new HashSet<>();

    PoliteFrontier(double defaultDelaySeconds) { this.defaultDelay = defaultDelaySeconds; }

    /**
     * One canonical form per page: lower-case scheme/host, default ports and fragments
     * dropped, tracking params removed, params sorted.
     */
    static String normalise(String url) {
        URI u = URI.create(url.trim());
        String scheme = u.getScheme().toLowerCase(), host = u.getHost().toLowerCase();
        int port = u.getPort();
        boolean defaultPort = port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
        String path = u.getRawPath() == null || u.getRawPath().isEmpty() ? "/" : u.getRawPath();
        String query = u.getRawQuery() == null ? "" : Arrays.stream(u.getRawQuery().split("&"))
                .filter(kv -> !kv.isEmpty() && !TRACKING.contains(kv.split("=", 2)[0]))
                .sorted().collect(Collectors.joining("&"));
        return scheme + "://" + host + (defaultPort ? "" : ":" + port) + path + (query.isEmpty() ? "" : "?" + query);
    }

    boolean add(String url, double now) {
        String u = normalise(url);
        if (!seen.add(u)) return false;
        String host = URI.create(u).getHost();
        Deque<String> q = queues.computeIfAbsent(host, h -> new ArrayDeque<>());
        if (q.isEmpty()) ready.add(new Ready(Math.max(now, nextAllowed.getOrDefault(host, now)), host));
        q.addLast(u);
        return true;
    }

    /**
     * The next URL whose host may be contacted now, or null if every host is cooling
     * down.
     */
    String next(double now) {
        Ready r = ready.peek();
        if (r == null || r.at() > now) return null;
        ready.poll();
        Deque<String> q = queues.get(r.host());
        String url = q.pollFirst();
        nextAllowed.put(r.host(), now + delay.getOrDefault(r.host(), defaultDelay));
        if (!q.isEmpty()) ready.add(new Ready(nextAllowed.get(r.host()), r.host()));
        return url;
    }
}
