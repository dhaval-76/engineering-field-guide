class Stack:
    """Has-a list (composition), not is-a list: callers can't insert into the middle or reorder."""

    def __init__(self):
        self._items: list = []

    def push(self, item) -> None:
        self._items.append(item)

    def pop(self):
        return self._items.pop()

    def peek(self):
        return self._items[-1]

    @property
    def is_empty(self) -> bool:
        return not self._items
