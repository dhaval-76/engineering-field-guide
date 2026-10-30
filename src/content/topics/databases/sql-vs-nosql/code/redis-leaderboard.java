@Service
class Leaderboard {
    private static final String KEY = "leaderboard:season-12";
    private final StringRedisTemplate redis;

    Leaderboard(StringRedisTemplate redis) { this.redis = redis; }

    void addPoints(String playerId, double points) {
        redis.opsForZSet().incrementScore(KEY, playerId, points);    // ZINCRBY — atomic, O(log n)
    }

    List<Entry> top(int n) {                                          // ZRANGE … REV WITHSCORES
        var tuples = redis.opsForZSet().reverseRangeWithScores(KEY, 0, n - 1);
        List<Entry> out = new ArrayList<>();
        int rank = 1;
        for (var t : tuples) out.add(new Entry(rank++, t.getValue(), t.getScore()));
        return out;
    }

    OptionalLong rankOf(String playerId) {                           // ZREVRANK, 0-based
        Long r = redis.opsForZSet().reverseRank(KEY, playerId);
        return r == null ? OptionalLong.empty() : OptionalLong.of(r + 1);
    }

    Set<String> withScoreBetween(double min, double max) {           // ZRANGE … BYSCORE
        return redis.opsForZSet().rangeByScore(KEY, min, max);
    }

    record Entry(int rank, String playerId, double score) {}
}
// Ties: equal scores are ordered by member name. For "earlier achiever wins", encode
// time into the score, e.g. score = points * 1e10 + (MAX_TS - achievedAtSeconds).
