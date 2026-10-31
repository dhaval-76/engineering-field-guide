# uses Parser from above
class LazySheet:
    """Mark dependents dirty on write, recompute on read (good when most cells are never viewed)."""

    def __init__(self):
        self._cells: dict[str, list] = {}           # id -> [expr, value]
        self._dependents: dict[str, set[str]] = {}
        self._dirty: set[str] = set()

    def set_formula(self, id: str, text: str) -> None:
        expr = Parser.parse(text)
        for r in expr.references():
            self._dependents.setdefault(r, set()).add(id)
        self._cells[id] = [expr, 0.0]
        self._mark_dirty(id)

    def _mark_dirty(self, id: str) -> None:
        stack = [id]
        while stack:
            c = stack.pop()
            if c not in self._dirty:
                self._dirty.add(c)
                stack.extend(self._dependents.get(c, ()))

    def get(self, id: str) -> float:
        cell = self._cells.get(id)
        if cell is None:
            return 0.0
        if id in self._dirty:
            self._dirty.discard(id)
            cell[1] = cell[0].eval(self.get)        # recursion pulls dirty inputs first
        return cell[1]
