public final class OrderSummaryService {
    private final ExecutorService io;
    private final PricingClient pricing;
    private final InventoryClient inventory;

    public OrderSummaryService(ExecutorService io, PricingClient pricing, InventoryClient inventory) {
        this.io = io; this.pricing = pricing; this.inventory = inventory;
    }

    public CompletableFuture<List<ItemView>> summarize(ImmutableOrder order) {
        List<CompletableFuture<ItemView>> perItem = order.items().stream()
            .map(this::enrich)
            .toList();

        return CompletableFuture.allOf(perItem.toArray(CompletableFuture[]::new))
            .thenApply(v -> perItem.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<ItemView> enrich(OrderItem item) {
        CompletableFuture<BigDecimal> price = supplyAsync(() -> pricing.priceOf(item.sku()), io)
            .orTimeout(300, MILLISECONDS);
        CompletableFuture<Boolean> inStock = supplyAsync(() -> inventory.inStock(item.sku(), item.qty()), io)
            .orTimeout(300, MILLISECONDS);

        return price.thenCombine(inStock, (p, ok) -> ok ? ItemView.available(item, p) : ItemView.unavailable(item))
            // one bad item never fails the whole response
            .exceptionally(ex -> ItemView.unavailable(item));
    }
}

public record ItemView(OrderItem item, BigDecimal price, Status status) {
    public enum Status { AVAILABLE, UNAVAILABLE }
    static ItemView available(OrderItem i, BigDecimal p) { return new ItemView(i, p, Status.AVAILABLE); }
    static ItemView unavailable(OrderItem i) { return new ItemView(i, null, Status.UNAVAILABLE); }
}
