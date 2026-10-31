final class Sheet {
    private final Map<String, Cell> cells = new HashMap<>();
    // A1 → {B1, C1}: who reads A1
    private final Map<String, Set<String>> dependents = new HashMap<>();

    /** Set a value ("42") or formula ("=A1+B1*2"). Rejects cycles without changing anything. */
    public void set(String id, String input) {
        Expr expr = input.startsWith("=") ? Parser.parse(input.substring(1)) : new Expr.Num(Double.parseDouble(input));
        Set<String> refs = expr.references();

        // check BEFORE mutating
        if (createsCycle(id, refs)) throw new CircularReferenceException(id);

        Cell cell = cells.computeIfAbsent(id, Cell::new);
        // drop old edges
        for (String old : cell.refs) dependents.getOrDefault(old, Set.of()).remove(id);
        // add new edges
        for (String r : refs) dependents.computeIfAbsent(r, k -> new HashSet<>()).add(id);
        cell.expr = expr;
        cell.refs = refs;

        recalculateFrom(id);
    }

    public double get(String id) { Cell c = cells.get(id); return c == null ? 0 : c.value; }

    /** Would `id` (directly or indirectly) end up depending on itself? DFS from its new references. */
    private boolean createsCycle(String id, Set<String> refs) {
        Deque<String> stack = new ArrayDeque<>(refs);
        Set<String> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (cur.equals(id)) return true;
            if (!seen.add(cur)) continue;
            Cell c = cells.get(cur);
            // follow what `cur` reads
            if (c != null) stack.addAll(c.refs);
        }
        return false;
    }

    /** Recompute the changed cell and everything downstream, in topological order (Kahn's algorithm). */
    private void recalculateFrom(String start) {
        // 1. collect the downstream subgraph
        Set<String> affected = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>(List.of(start));
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (affected.add(cur)) stack.addAll(dependents.getOrDefault(cur, Set.of()));
        }

        // 2. count edges inside that subgraph
        Map<String, Integer> inDegree = new HashMap<>();
        for (String c : affected) inDegree.putIfAbsent(c, 0);
        for (String c : affected)
            for (String d : dependents.getOrDefault(c, Set.of()))
                if (affected.contains(d)) inDegree.merge(d, 1, Integer::sum);

        // 3. evaluate cells whose inputs are all fresh
        Deque<String> ready = new ArrayDeque<>();
        inDegree.forEach((c, deg) -> { if (deg == 0) ready.add(c); });
        while (!ready.isEmpty()) {
            String c = ready.poll();
            Cell cell = cells.get(c);
            cell.value = cell.expr.eval(this::get);
            for (String d : dependents.getOrDefault(c, Set.of()))
                if (affected.contains(d) && inDegree.merge(d, -1, Integer::sum) == 0) ready.add(d);
        }
    }
}

final class Cell {
    final String id;
    Expr expr = new Expr.Num(0);
    Set<String> refs = Set.of();
    double value;
    Cell(String id) { this.id = id; }
}
