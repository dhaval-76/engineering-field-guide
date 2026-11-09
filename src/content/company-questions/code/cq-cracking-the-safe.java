String crackSafe(int n, int k) {
    if (n == 1) { // every digit once
        StringBuilder out = new StringBuilder();
        for (int d = 0; d < k; d++) out.append(d);
        return out.toString();
    }
    StringBuilder out = new StringBuilder();
    walk("0".repeat(n - 1), k, new HashSet<>(), out);
    return out.append("0".repeat(n - 1)).toString();
}

/** Euler circuit: nodes are n-1 digits, each edge adds one digit and is one password. */
void walk(String node, int k, Set<String> used, StringBuilder out) {
    for (int d = 0; d < k; d++) {
        String edge = node + d;
        if (used.add(edge)) {
            walk(edge.substring(1), k, used, out);
            out.append(d);
        }
    }
}
