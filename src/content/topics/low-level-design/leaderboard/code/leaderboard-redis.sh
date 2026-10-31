# Redis sorted set: a skip list with spans, so rank queries are O(log n).
ZINCRBY lb:global 50 alice          # add points (creates the member if missing)
ZINCRBY lb:global 80 bob
ZINCRBY lb:global 50 carol

ZREVRANGE lb:global 0 9 WITHSCORES  # top 10, highest first
ZREVRANK  lb:global carol           # 0-based rank from the top
ZSCORE    lb:global carol
ZREVRANGE lb:global 3 7 WITHSCORES  # "players around me": a player at index 5, ± 2 places

# Ties: equal scores are ordered by member name (reversed by ZREVRANGE: carol before alice).
# To favour whoever reached the score first,
# encode time into the score:  score * 10^10 + (9999999999 - epochSeconds)
# (scores are doubles: exact only while the result stays below 2^53, i.e. score < ~900,000)

# Time-windowed boards: one key per period, expire old ones
ZINCRBY lb:daily:2026-10-31 50 alice
EXPIRE  lb:daily:2026-10-31 691200  # keep 8 days

# Weekly board = union of daily boards
ZUNIONSTORE lb:weekly:2026-W44 7 lb:daily:2026-10-26 lb:daily:2026-10-27 lb:daily:2026-10-28 \
  lb:daily:2026-10-29 lb:daily:2026-10-30 lb:daily:2026-10-31 lb:daily:2026-11-01
