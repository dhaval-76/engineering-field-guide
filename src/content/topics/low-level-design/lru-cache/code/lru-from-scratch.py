class _Node:
    __slots__ = ("key", "value", "prev", "next")

    def __init__(self, key=None, value=None):
        self.key = key  # kept so eviction can also delete the dict entry
        self.value = value
        self.prev = self.next = None


class LRUCache:
    def __init__(self, capacity: int):
        if capacity <= 0:
            raise ValueError("capacity must be > 0")
        self._capacity = capacity
        self._index: dict = {}
        # Sentinels: head.next is the most recently used, tail.prev the least.
        self._head, self._tail = _Node(), _Node()
        self._head.next, self._tail.prev = self._tail, self._head

    def get(self, key):
        node = self._index.get(key)
        if node is None:
            return None
        self._unlink(node)
        self._push_front(node)
        return node.value

    def put(self, key, value) -> None:
        node = self._index.get(key)
        if node is not None:
            node.value = value
            self._unlink(node)
            self._push_front(node)
            return
        if len(self._index) == self._capacity:
            lru = self._tail.prev
            self._unlink(lru)
            del self._index[lru.key]
        node = _Node(key, value)
        self._index[key] = node
        self._push_front(node)

    def _push_front(self, n):
        n.prev, n.next = self._head, self._head.next
        self._head.next.prev = n
        self._head.next = n

    def _unlink(self, n):
        n.prev.next = n.next
        n.next.prev = n.prev
