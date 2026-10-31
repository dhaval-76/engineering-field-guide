import threading


class WaitingRoom:
    """Virtual waiting room: a place in line for everyone, admission at a controlled rate, expiring sessions."""

    def __init__(self, admit_per_tick: int, session_seconds: float):
        self.admit_per_tick = admit_per_tick
        self.session_seconds = session_seconds
        self._seq_of: dict[str, int] = {}
        self._admitted_at: dict[int, float] = {}
        self._next_seq = 0
        self._admitted_up_to = 0  # seq < admitted_up_to are admitted
        self._lock = threading.Lock()

    def join(self, user_id: str) -> int:
        with self._lock:
            if user_id not in self._seq_of:  # refreshing keeps your place
                self._seq_of[user_id] = self._next_seq
                self._next_seq += 1
            return self._seq_of[user_id]

    def tick(self, now: float) -> None:
        """Called on a timer: admit the next batch in arrival order."""
        with self._lock:
            up_to = min(self._next_seq, self._admitted_up_to + self.admit_per_tick)
            for s in range(self._admitted_up_to, up_to):
                self._admitted_at[s] = now
            self._admitted_up_to = up_to

    def position(self, user_id: str) -> int:
        return max(0, self._seq_of[user_id] - self._admitted_up_to + 1)

    def may_book(self, user_id: str, now: float) -> bool:
        seq = self._seq_of.get(user_id)
        if seq is None or seq >= self._admitted_up_to:
            return False
        return now - self._admitted_at[seq] <= self.session_seconds
