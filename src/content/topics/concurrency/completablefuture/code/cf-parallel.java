// Three independent calls: run them together, wait for all, combine. Wall time ≈ the
// slowest call.
// dedicated pool for blocking I/O
private final ExecutorService io = Executors.newFixedThreadPool(50);

public ProductPage buildPage(String productId) {
    CompletableFuture<Product>   product = supplyAsync(() -> productService.get(productId), io)
                                               // Java 9+
                                               .orTimeout(300, MILLISECONDS);
    CompletableFuture<Inventory> stock   = supplyAsync(() -> inventoryService.get(productId), io)
                                               .completeOnTimeout(Inventory.UNKNOWN, 300, MILLISECONDS);
    CompletableFuture<Price>     price   = supplyAsync(() -> pricingService.get(productId), io)
                                               .orTimeout(300, MILLISECONDS);

    return CompletableFuture.allOf(product, stock, price)
        // join() won't block here
        .thenApply(ignored -> new ProductPage(product.join(), stock.join(), price.join()))
        .join();   // or return the CompletableFuture itself from an async controller
}
