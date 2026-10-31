import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A rule matches when the user's attribute is one of the values.
record Rule(String attribute, Set<String> values, String variation) {}

// One slice of a percentage rollout; weights are out of 100,000 (so 0.001% steps).
record Slice(String variation, int weight) {}

record Flag(
        String key,
        boolean on,
        String salt,
        String offVariation,
        Map<String, String> targets, // userId -> variation
        List<Rule> rules,
        List<Slice> rollout) {}

record Result(String variation, String reason) {}

final class FlagEvaluator {
    static final int BUCKETS = 100_000;

    // The same user always gets the same bucket for a flag, in every SDK language.
    static int bucket(String flagKey, String salt, String userId) {
        try {
            byte[] h =
                    MessageDigest.getInstance("SHA-256")
                            .digest((flagKey + ":" + salt + ":" + userId)
                                    .getBytes(StandardCharsets.UTF_8));
            long first32 =
                    ((h[0] & 0xFFL) << 24) | ((h[1] & 0xFFL) << 16)
                            | ((h[2] & 0xFFL) << 8) | (h[3] & 0xFFL);
            return (int) (first32 % BUCKETS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Result evaluate(Flag flag, String userId, Map<String, String> attrs) {
        if (!flag.on()) return new Result(flag.offVariation(), "OFF");
        String target = flag.targets().get(userId);
        if (target != null) return new Result(target, "TARGET");
        for (int i = 0; i < flag.rules().size(); i++) {
            Rule r = flag.rules().get(i);
            if (r.values().contains(attrs.get(r.attribute())))
                return new Result(r.variation(), "RULE " + i);
        }
        // Walk the slices: bucket 0..99,999 falls into exactly one of them.
        int b = bucket(flag.key(), flag.salt(), userId), upTo = 0;
        for (Slice s : flag.rollout()) {
            upTo += s.weight();
            if (b < upTo) return new Result(s.variation(), "ROLLOUT");
        }
        return new Result(flag.offVariation(), "ROLLOUT");
    }
}
