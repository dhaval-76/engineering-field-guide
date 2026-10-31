enum Channel { PUSH, SMS, EMAIL, IN_APP }
enum Category { SECURITY, TRANSACTIONAL, SOCIAL, MARKETING }

record Notification(String id, String userId, Category category, String dedupKey, List<Channel> preferredChannels) {}
record Prefs(Set<Channel> optedOut, Set<Category> mutedCategories, LocalTime quietStart, LocalTime quietEnd, ZoneId zone) {}
record Decision(List<Channel> sendNow, List<Channel> deferred, String reason) {}

/**
 * Decides where a notification goes, applying dedup, preferences, quiet hours and
 * frequency caps.
 */
class NotificationRouter {
    // Redis SET NX EX in production
    private final Map<String, Long> seenDedupKeys = new HashMap<>();
    // per-user send times
    private final Map<String, Deque<Long>> marketingSent = new HashMap<>();
    private final int marketingPerDay;

    NotificationRouter(int marketingPerDay) { this.marketingPerDay = marketingPerDay; }

    synchronized Decision route(Notification n, Prefs p, Instant now) {
        long t = now.getEpochSecond();
        Long seen = seenDedupKeys.get(n.dedupKey());
        if (seen != null && t - seen < 86_400) return new Decision(List.of(), List.of(), "duplicate");
        seenDedupKeys.put(n.dedupKey(), t);

        // OTPs, password resets
        boolean critical = n.category() == Category.SECURITY;
        if (!critical && p.mutedCategories().contains(n.category()))
            return new Decision(List.of(), List.of(), "muted by user");

        if (n.category() == Category.MARKETING) {
            Deque<Long> sent = marketingSent.computeIfAbsent(n.userId(), k -> new ArrayDeque<>());
            while (!sent.isEmpty() && t - sent.peekFirst() >= 86_400) sent.pollFirst();
            if (sent.size() >= marketingPerDay) return new Decision(List.of(), List.of(), "frequency cap");
            sent.addLast(t);
        }

        List<Channel> allowed = n.preferredChannels().stream()
                .filter(c -> critical || !p.optedOut().contains(c)).toList();
        if (allowed.isEmpty()) return new Decision(List.of(), List.of(), "no allowed channel");

        boolean quiet = !critical && inQuietHours(now, p);
        List<Channel> sendNow = new ArrayList<>(), later = new ArrayList<>();
        for (Channel c : allowed) {
            // in-app inbox is silent anyway
            if (quiet && c != Channel.IN_APP) later.add(c);
            else sendNow.add(c);
        }
        return new Decision(sendNow, later, quiet ? "quiet hours" : "ok");
    }

    private static boolean inQuietHours(Instant now, Prefs p) {
        if (p.quietStart() == null) return false;
        LocalTime local = now.atZone(p.zone()).toLocalTime();
        return p.quietStart().isBefore(p.quietEnd())
                ? !local.isBefore(p.quietStart()) && local.isBefore(p.quietEnd())
                // window crosses midnight
                : !local.isBefore(p.quietStart()) || local.isBefore(p.quietEnd());
    }
}
