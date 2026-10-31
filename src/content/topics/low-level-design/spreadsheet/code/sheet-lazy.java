/**
 * Alternative: mark dependents dirty on write, recompute on read (good when most cells
 * are never viewed).
 */
final class LazySheet {
    private final Map<String, Cell> cells = new HashMap<>();
    private final Map<String, Set<String>> dependents = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();

    void setValue(String id, double v) {
        cells.computeIfAbsent(id, Cell::new).expr = new Expr.Num(v);
        markDirty(id);
    }

    private void markDirty(String id) {
        Deque<String> stack = new ArrayDeque<>(List.of(id));
        while (!stack.isEmpty()) {
            String c = stack.pop();
            if (dirty.add(c)) stack.addAll(dependents.getOrDefault(c, Set.of()));
        }
    }

    double get(String id) {
        Cell c = cells.get(id);
        if (c == null) return 0;
        // recursion pulls dirty inputs first
        if (dirty.remove(id)) c.value = c.expr.eval(this::get);
        return c.value;
    }
}
// Incremental aggregates: SUM over a range can be updated in O(1) on a single-cell change
// (newSum = oldSum − oldValue + newValue) instead of re-adding every cell.
