public final class Order {                                   // 1. final: no mutable subclasses
    private final String id;                                 // 2. private final fields
    private final List<LineItem> items;
    private final Instant createdAt;

    public Order(String id, List<LineItem> items, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.items = List.copyOf(items);                     // 3. defensive copy of mutable input (also rejects nulls)
        this.createdAt = Objects.requireNonNull(createdAt);   //    Instant is already immutable
    }

    public String id() { return id; }
    public List<LineItem> items() { return items; }          // 4. List.copyOf is unmodifiable → safe to return
    public Instant createdAt() { return createdAt; }         // 5. no setters

    public Order withItem(LineItem item) {                   // 6. "modification" returns a new object
        var next = new ArrayList<>(items);
        next.add(item);
        return new Order(id, next, createdAt);
    }
}
// Fields of mutable types (Date, arrays, ArrayList) must be copied on the way in AND on the way out
// if they can't be made unmodifiable — otherwise callers can change "immutable" state.
