from collections import OrderedDict


class LRUCache:
    """OrderedDict keeps insertion order; move_to_end() marks a key as most recent."""

    def __init__(self, capacity: int):
        self._capacity = capacity
        self._data: OrderedDict = OrderedDict()

    def get(self, key):
        if key not in self._data:
            return None
        self._data.move_to_end(key)
        return self._data[key]

    def put(self, key, value) -> None:
        self._data[key] = value
        self._data.move_to_end(key)
        if len(self._data) > self._capacity:
            self._data.popitem(last=False)  # pop the least recently used


# For memoising a function, the standard library already has it:
# from functools import lru_cache
# @lru_cache(maxsize=1024)
# def load_profile(user_id): ...
