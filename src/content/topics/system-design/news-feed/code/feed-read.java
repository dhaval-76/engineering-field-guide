@Service
class FeedService {
    private static final int PAGE = 20;
    private final StringRedisTemplate redis;
    private final FollowGraph graph;
    private final PostStore posts;

    /** cursor == null → first page; otherwise items strictly older than the cursor. */
    FeedPage read(long userId, Cursor cursor) {
        double maxScore = cursor == null ? Double.POSITIVE_INFINITY : cursor.score();

        // 1. Pushed ids from the user's feed cache (older than the cursor, newest first).
        Set<ZSetOperations.TypedTuple<String>> cached = redis.opsForZSet()
            .reverseRangeByScoreWithScores("feed:" + userId, Double.NEGATIVE_INFINITY, maxScore, 0, PAGE * 2);

        List<Candidate> candidates = new ArrayList<>();
        if (cached != null) cached.forEach(t -> candidates.add(new Candidate(Long.parseLong(t.getValue()), t.getScore())));

        // 2. Pulled posts from celebrities this user follows (a handful of indexed
        // reads).
        for (long celebrity : graph.celebritiesFollowedBy(userId))
            posts.latestByAuthor(celebrity, maxScore, 5)
                 .forEach(p -> candidates.add(new Candidate(p.id(), p.createdAtMillis())));

        // 3. Drop the exact cursor item (ties), sort, take a page.
        List<Candidate> page = candidates.stream()
            .filter(c -> cursor == null || c.score() < cursor.score()
                      || (c.score() == cursor.score() && c.postId() < cursor.postId()))
            .sorted(Comparator.comparingDouble(Candidate::score).reversed()
                    .thenComparing(Candidate::postId, Comparator.reverseOrder()))
            .distinct()
            .limit(PAGE)
            .toList();

        // 4. Hydrate in one batch; filter deleted posts and blocked authors at read time.
        List<PostView> views = posts.getMany(page.stream().map(Candidate::postId).toList()).stream()
            .filter(p -> !p.deleted() && !graph.isBlocked(userId, p.authorId()))
            .toList();

        Cursor next = page.isEmpty() ? null : new Cursor(page.getLast().score(), page.getLast().postId());
        return new FeedPage(views, next);
    }

    record Candidate(long postId, double score) {}
    record Cursor(double score, long postId) {}
}
