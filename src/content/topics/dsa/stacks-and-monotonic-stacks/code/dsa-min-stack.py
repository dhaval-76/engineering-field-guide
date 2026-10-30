class MinStack:
    def __init__(self):
        self._stack = []                         # (value, min so far)

    def push(self, x: int) -> None:
        self._stack.append((x, min(x, self._stack[-1][1]) if self._stack else x))

    def pop(self) -> None:
        self._stack.pop()

    def top(self) -> int:
        return self._stack[-1][0]

    def get_min(self) -> int:
        return self._stack[-1][1]
