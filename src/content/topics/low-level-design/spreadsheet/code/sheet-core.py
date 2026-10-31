from collections import deque
from typing import Callable

class Sheet:
    """Each cell keeps its references; a reverse map of dependents drives ordered recalculation."""

    def __init__(self):
        self.cells: dict[str, dict] = {}            # id -> {"refs", "fn", "value"}
        self.dependents: dict[str, set[str]] = {}   # id -> ids that read it

    def set(self, id: str, refs: set[str], fn: Callable[[Callable[[str], float]], float]) -> None:
        if self._creates_cycle(id, refs):
            raise ValueError(f"circular reference at {id}")
        for r in self.cells.get(id, {}).get("refs", set()):
            self.dependents.get(r, set()).discard(id)
        for r in refs:
            self.dependents.setdefault(r, set()).add(id)
        self.cells[id] = {"refs": set(refs), "fn": fn, "value": 0.0}
        self._recalc(id)

    def get(self, id: str) -> float:
        return self.cells.get(id, {}).get("value", 0.0)

    def _creates_cycle(self, id: str, refs) -> bool:
        stack, seen = list(refs), set()
        while stack:
            cur = stack.pop()
            if cur == id:
                return True
            if cur not in seen:
                seen.add(cur)
                stack.extend(self.cells.get(cur, {}).get("refs", ()))
        return False

    def _recalc(self, start: str) -> None:
        """Topological order (Kahn) over the cells affected by `start`, so each is computed once."""
        affected, stack = set(), [start]
        while stack:
            c = stack.pop()
            if c not in affected:
                affected.add(c)
                stack.extend(self.dependents.get(c, ()))
        indeg = {c: 0 for c in affected}
        for c in affected:
            for d in self.dependents.get(c, ()):
                if d in affected:
                    indeg[d] += 1
        ready = deque(c for c in affected if indeg[c] == 0)
        while ready:
            c = ready.popleft()
            if c in self.cells:
                self.cells[c]["value"] = self.cells[c]["fn"](self.get)
            for d in self.dependents.get(c, ()):
                if d in affected:
                    indeg[d] -= 1
                    if indeg[d] == 0:
                        ready.append(d)
