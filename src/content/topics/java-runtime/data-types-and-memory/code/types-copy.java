record Item(String sku, int qty) {}                 // immutable: safe to share between copies

final class Order {
    final List<Item> items;
    final Map<String, String> notes;
    Order(List<Item> items, Map<String, String> notes) { this.items = items; this.notes = notes; }

    /** Shallow copy: new Order, SAME list and map → changes leak between the two. */
    Order shallowCopy() { return new Order(items, notes); }

    /** Deep enough copy: new containers; elements are immutable so they can be shared. */
    Order copy() { return new Order(new ArrayList<>(items), new HashMap<>(notes)); }
}

// When elements are mutable (e.g. int[]), copy them too:
static List<int[]> deepCopy(List<int[]> src) {
    return src.stream().map(a -> Arrays.copyOf(a, a.length)).collect(Collectors.toCollection(ArrayList::new));
}
// Prefer immutable data (records, List.copyOf) so copies are rarely needed at all.
