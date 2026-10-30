// ioredis
import Redis from 'ioredis';
const redis = new Redis();
const KEY = 'leaderboard:season-12';

export const addPoints = (playerId, points) => redis.zincrby(KEY, points, playerId);

export async function top(n) {
  const flat = await redis.zrange(KEY, 0, n - 1, 'REV', 'WITHSCORES');   // [id, score, id, score…]
  const out = [];
  for (let i = 0; i < flat.length; i += 2) {
    out.push({ rank: i / 2 + 1, playerId: flat[i], score: Number(flat[i + 1]) });
  }
  return out;
}

export async function rankOf(playerId) {
  const r = await redis.zrevrank(KEY, playerId);
  return r === null ? null : r + 1;
}
