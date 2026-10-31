// Redis variant for very hot shows (ioredis): one key per held seat, auto-expiring.
const RELEASE_IF_OWNER = `if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end`;

// SET key value NX PX ttl — succeeds only if nobody holds the seat; expires on its own
export async function holdSeat(redis, showId, seatId, holdId) {
  return (await redis.set(`hold:${showId}:${seatId}`, holdId, 'PX', 8 * 60 * 1000, 'NX')) === 'OK';
}

// Release only if WE still hold it (compare-and-delete must be atomic → Lua)
export async function releaseSeat(redis, showId, seatId, holdId) {
  return (await redis.eval(RELEASE_IF_OWNER, 1, `hold:${showId}:${seatId}`, holdId)) === 1;
}
// The database row is still the source of truth for BOOKED; Redis only arbitrates short-lived holds.
