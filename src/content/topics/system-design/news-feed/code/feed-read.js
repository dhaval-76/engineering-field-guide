// Hybrid feed read: pushed ids from the user's Redis sorted set + pulled posts from
// followed celebrities.
const PAGE = 20;

// cursor: null for the first page, otherwise { score, postId } of the last item shown
export function mergePage(cached, pulled, cursor) {
  const older = (c) =>
    !cursor || c.score < cursor.score || (c.score === cursor.score && c.postId < cursor.postId);
  const seen = new Set();
  const page = [...cached, ...pulled]
    .filter(older)
    .sort((a, b) => b.score - a.score || b.postId - a.postId)
    .filter((c) => !seen.has(c.postId) && seen.add(c.postId))
    .slice(0, PAGE);
  return {
    page,
    next: page.length ? { score: page.at(-1).score, postId: page.at(-1).postId } : null,
  };
}

export async function readFeed(userId, cursor) {
  const max = cursor ? cursor.score : '+inf';
  const raw = await redis.zrevrangebyscore(
    `feed:${userId}`,
    max,
    '-inf',
    'WITHSCORES',
    'LIMIT',
    0,
    PAGE * 2,
  );
  const cached = [];
  for (let i = 0; i < raw.length; i += 2)
    cached.push({ postId: Number(raw[i]), score: Number(raw[i + 1]) });
  const pulled = (
    await Promise.all(
      (await graph.celebritiesFollowedBy(userId)).map((c) =>
        posts.latestByAuthor(c, cursor?.score ?? Infinity, 5),
      ),
    )
  )
    .flat()
    .map((p) => ({ postId: p.id, score: p.createdAtMillis }));
  const { page, next } = mergePage(cached, pulled, cursor);
  // hydrate in one batch
  const views = (await posts.getMany(page.map((c) => c.postId)))
    // filter at read time
    .filter((p) => !p.deleted && !graph.isBlocked(userId, p.authorId));
  return { views, next };
}
