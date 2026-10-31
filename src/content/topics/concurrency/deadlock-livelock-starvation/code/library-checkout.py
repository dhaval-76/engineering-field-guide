import threading

class Library:
    def __init__(self):
        self._checked_out: dict[str, str] = {}          # book_id → user_id
        self._guard = threading.Lock()                   # protects the dict and the lock registry
        self._locks: dict[str, threading.Lock] = {}

    def checkout(self, user_id: str, book_id: str) -> bool:
        """At most one user can hold a book."""
        with self._guard:
            if book_id in self._checked_out:
                return False
            self._checked_out[book_id] = user_id
            return True

    def return_book(self, user_id: str, book_id: str) -> bool:
        with self._guard:
            if self._checked_out.get(book_id) != user_id:   # only the holder can return it
                return False
            del self._checked_out[book_id]
            return True

    def swap(self, book_a: str, book_b: str) -> None:
        """Swap two books' holders: take the per-book locks in a global (sorted) order — no deadlock."""
        first, second = sorted((book_a, book_b))
        with self._guard:
            l1 = self._locks.setdefault(first, threading.Lock())
            l2 = self._locks.setdefault(second, threading.Lock())
        with l1, l2:
            with self._guard:
                ua, ub = self._checked_out.get(book_a), self._checked_out.get(book_b)
                if ua and ub:
                    self._checked_out[book_a], self._checked_out[book_b] = ub, ua
# In a multi-instance service the database is the source of truth:
# UPDATE books SET holder = %s WHERE id = %s AND holder IS NULL  → 1 row = success.
