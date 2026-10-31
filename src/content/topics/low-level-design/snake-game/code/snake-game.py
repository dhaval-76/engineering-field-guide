from collections import deque

DIRS = {"UP": (-1, 0), "DOWN": (1, 0), "LEFT": (0, -1), "RIGHT": (0, 1)}

class SnakeGame:
    """Deque for the body (O(1) at both ends) + a set for O(1) self-collision checks."""

    def __init__(self, rows: int, cols: int, food_sequence: list[tuple[int, int]]):
        self.rows, self.cols = rows, cols
        self.food = deque(map(tuple, food_sequence))
        self.body = deque([(0, 0)])            # body[0] = head
        self.occupied = {(0, 0)}
        self.score, self.over = 0, False

    def move(self, direction: str) -> int:
        if self.over:
            return -1
        dr, dc = DIRS[direction]
        nxt = (self.body[0][0] + dr, self.body[0][1] + dc)
        if not (0 <= nxt[0] < self.rows and 0 <= nxt[1] < self.cols):
            return self._end()
        eats = bool(self.food) and self.food[0] == nxt
        if not eats:
            # the tail leaves first, so following it is legal
            self.occupied.discard(self.body.pop())
        if nxt in self.occupied:
            return self._end()
        self.body.appendleft(nxt)
        self.occupied.add(nxt)
        if eats:
            self.food.popleft()
            self.score += 1
        return self.score

    def _end(self) -> int:
        self.over = True
        return -1
