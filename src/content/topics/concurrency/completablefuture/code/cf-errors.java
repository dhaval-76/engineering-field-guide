CompletableFuture<Price> price = supplyAsync(() -> pricing.get(id), io)
    .orTimeout(200, MILLISECONDS)
    // exceptionally: recover from a failure with a fallback value
    .exceptionally(ex -> Price.unavailable())
    ;

CompletableFuture<Price> logged = supplyAsync(() -> pricing.get(id), io)
    // handle: always runs, sees either the value or the exception, returns a new value
    .handle((value, ex) -> {
        if (ex != null) {
            log.warn("pricing failed for {}", id, ex);   // ex is usually a CompletionException; getCause() has the original
            return Price.unavailable();
        }
        return value;
    });

// whenComplete: observe (metrics, logging) without changing the outcome
logged.whenComplete((v, ex) -> metrics.record("pricing", ex == null));

// join() throws unchecked CompletionException; get() throws checked ExecutionException.
// A failure in any stage skips the following thenApply/thenCompose stages until something handles it.
