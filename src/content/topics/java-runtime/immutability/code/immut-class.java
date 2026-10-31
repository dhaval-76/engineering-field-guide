// 1. final: no mutable subclasses
public final class Order {
    private final String id;                                 // 2. private final fields
    private final List<LineItem> items;
    private final Instant createdAt;

    public Order(String id, List<LineItem> items, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        // 3. defensive copy of mutable input (also rejects nulls)
        this.items = List.copyOf(items);
        // Instant is already immutable
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String id() { return id; }
    // 4. List.copyOf is unmodifiable → safe to return
    public List<LineItem> items() { return items; }
    public Instant createdAt() { return createdAt; }         // 5. no setters

    // 6. "modification" returns a new object
    public Order withItem(LineItem item) {
        var next = new ArrayList<>(items);
        next.add(item);
        return new Order(id, next, createdAt);
    }
}
// Fields of mutable types (Date, arrays, ArrayList) must be copied on the way in AND on
// the way out if they can't be made unmodifiable — otherwise callers can change
// "immutable" state.
