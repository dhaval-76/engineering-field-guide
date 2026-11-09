/** One selected field. listSize is the page size for a list field, or 0 for a single value. */
record Selection(String name, int listSize, List<Selection> children) {
    static Selection leaf(String name) {
        return new Selection(name, 0, List.of());
    }
}

record Cost(long fields, int depth, int nestedLists) {}

/** The number of fields the server would resolve if every list came back full. */
Cost analyze(List<Selection> selections) {
    long fields = 0;
    int depth = 0, lists = 0;
    for (Selection s : selections) {
        Cost below = analyze(s.children());
        long multiplier = Math.max(1, s.listSize());
        long subtree = saturatingAdd(1, saturatingMultiply(multiplier, below.fields()));
        fields = saturatingAdd(fields, subtree);
        depth = Math.max(depth, 1 + below.depth());
        lists = Math.max(lists, (s.listSize() > 0 ? 1 : 0) + below.nestedLists());
    }
    return new Cost(fields, depth, lists);
}

private long saturatingAdd(long a, long b) {
    return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
}

private long saturatingMultiply(long a, long b) {
    return b != 0 && a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
}

/** Null means allowed. Runs before any resolver does. */
String rejectionReason(List<Selection> query, long maxFields, int maxDepth, int maxNestedLists) {
    Cost cost = analyze(query);
    if (cost.depth() > maxDepth) return "too deep: " + cost.depth();
    if (cost.nestedLists() > maxNestedLists) return "too many nested lists: " + cost.nestedLists();
    if (cost.fields() > maxFields) return "too expensive: " + cost.fields() + " fields";
    return null;
}
