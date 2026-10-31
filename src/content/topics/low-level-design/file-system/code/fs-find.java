/**
 * Each find option is a small predicate; and/or/not compose them (Specification pattern).
 */
@FunctionalInterface
interface Filter {
    boolean test(Node n);
    default Filter and(Filter o) { return n -> test(n) && o.test(n); }
    default Filter or(Filter o)  { return n -> test(n) || o.test(n); }
    default Filter negate()      { return n -> !test(n); }

    static Filter name(String glob)   { Pattern p = Glob.toRegex(glob); return n -> p.matcher(n.name).matches(); }
    static Filter type(boolean dirs)  { return n -> (n instanceof DirNode) == dirs; }
    static Filter sizeAtLeast(long b) { return n -> n instanceof FileNode && n.size() >= b; }
    static Filter modifiedWithin(Duration d, Clock c) { return n -> n.modified.isAfter(c.instant().minus(d)); }
}

final class Glob {
    /** "*.java" → regex; escape everything except the glob wildcards * and ?. */
    static Pattern toRegex(String glob) {
        StringBuilder re = new StringBuilder();
        for (char ch : glob.toCharArray()) {
            switch (ch) {
                case '*' -> re.append("[^/]*");
                case '?' -> re.append("[^/]");
                default  -> re.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return Pattern.compile(re.toString());
    }
}

final class Find {
    /** Iterative DFS with an explicit stack — no recursion depth limit on deep trees. */
    static List<Node> find(Node start, Filter filter, int maxDepth) {
        List<Node> out = new ArrayList<>();
        Deque<Map.Entry<Node, Integer>> stack = new ArrayDeque<>(List.of(Map.entry(start, 0)));
        while (!stack.isEmpty()) {
            var e = stack.pop();
            Node n = e.getKey();
            if (filter.test(n)) out.add(n);
            if (n instanceof DirNode d && e.getValue() < maxDepth)
                d.children().forEach(c -> stack.push(Map.entry(c, e.getValue() + 1)));
        }
        return out;
    }
}

// find /src -name "*.java" -size +1k ! -name "*Test*"
List<Node> hits = Find.find(fs.resolve("/src"),
        Filter.name("*.java").and(Filter.sizeAtLeast(1024)).and(Filter.name("*Test*").negate()), Integer.MAX_VALUE);
