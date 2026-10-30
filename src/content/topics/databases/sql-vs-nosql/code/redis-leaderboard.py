# redis-py
import redis

r = redis.Redis(decode_responses=True)
KEY = "leaderboard:season-12"

def add_points(player_id: str, points: float) -> None:
    r.zincrby(KEY, points, player_id)

def top(n: int) -> list[dict]:
    rows = r.zrange(KEY, 0, n - 1, desc=True, withscores=True)   # [(id, score), …]
    return [{"rank": i + 1, "player_id": pid, "score": s} for i, (pid, s) in enumerate(rows)]

def rank_of(player_id: str) -> int | None:
    rank = r.zrevrank(KEY, player_id)
    return None if rank is None else rank + 1
