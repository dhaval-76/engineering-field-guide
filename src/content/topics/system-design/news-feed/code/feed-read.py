# Hybrid feed read: pushed ids from the user's Redis sorted set + pulled posts from followed celebrities.
PAGE = 20

def merge_page(cached: list[tuple[int, float]], pulled: list[tuple[int, float]], cursor):
    """Items are (post_id, score); cursor is None or (score, post_id) of the last item shown."""
    def older(c):
        return cursor is None or c[1] < cursor[0] or (c[1] == cursor[0] and c[0] < cursor[1])
    seen, page = set(), []
    for c in sorted(filter(older, cached + pulled), key=lambda c: (-c[1], -c[0])):
        if c[0] not in seen:
            seen.add(c[0])
            page.append(c)
            if len(page) == PAGE:
                break
    nxt = (page[-1][1], page[-1][0]) if page else None
    return page, nxt

def read_feed(user_id: int, cursor=None):
    max_score = cursor[0] if cursor else "+inf"
    cached = [(int(m), s) for m, s in redis.zrevrangebyscore(f"feed:{user_id}", max_score, "-inf",
                                                              start=0, num=PAGE * 2, withscores=True)]
    pulled = [(p.id, p.created_at_millis)
              for celeb in graph.celebrities_followed_by(user_id)
              for p in posts.latest_by_author(celeb, cursor[0] if cursor else float("inf"), 5)]
    page, nxt = merge_page(cached, pulled, cursor)
    views = [p for p in posts.get_many([c[0] for c in page])          # hydrate in one batch
             if not p.deleted and not graph.is_blocked(user_id, p.author_id)]   # filter at read time
    return views, nxt
