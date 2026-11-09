class Rates {
    private final Map<String, String> parent = new HashMap<>();
    private final Map<String, Double> toParent = new HashMap<>(); // 1 x = this many parents

    private String find(String x) {
        parent.putIfAbsent(x, x);
        toParent.putIfAbsent(x, 1.0);
        String p = parent.get(x);
        if (!p.equals(x)) {
            String root = find(p);
            toParent.put(x, toParent.get(x) * toParent.get(p)); // now relative to the root
            parent.put(x, root);
        }
        return parent.get(x);
    }

    /** Records that 1 a = rate b. */
    void add(String a, double rate, String b) {
        String ra = find(a), rb = find(b);
        if (ra.equals(rb)) return;
        parent.put(ra, rb);
        toParent.put(ra, rate * toParent.get(b) / toParent.get(a));
    }

    /** Returns how many b one a is worth, or -1 if they aren't connected. */
    double convert(String a, String b) {
        if (!parent.containsKey(a) || !parent.containsKey(b)) return -1;
        if (!find(a).equals(find(b))) return -1;
        return toParent.get(a) / toParent.get(b);
    }
}
