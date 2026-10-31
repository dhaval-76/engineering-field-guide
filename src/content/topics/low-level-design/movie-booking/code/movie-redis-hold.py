# Redis variant for very hot shows (redis-py): one key per held seat, auto-expiring.
import redis

r = redis.Redis()
RELEASE_IF_OWNER = r.register_script(
    "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end")

def hold_seat(show_id: str, seat_id: str, hold_id: str) -> bool:
    # SET key value NX PX ttl — succeeds only if nobody holds the seat; expires on its own
    return bool(r.set(f"hold:{show_id}:{seat_id}", hold_id, nx=True, px=8 * 60 * 1000))

def release_seat(show_id: str, seat_id: str, hold_id: str) -> bool:
    # release only if WE still hold it (compare-and-delete must be atomic → Lua)
    return RELEASE_IF_OWNER(keys=[f"hold:{show_id}:{seat_id}"], args=[hold_id]) == 1
# The database row is still the source of truth for BOOKED; Redis only arbitrates short-lived holds.
