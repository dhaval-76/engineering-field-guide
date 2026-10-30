CompletableFuture<User> user = supplyAsync(() -> users.find(id), io);

// thenApply: transform the value (like map). Runs on whichever thread completed the previous stage.
CompletableFuture<String> name = user.thenApply(User::displayName);

// thenCompose: the next step is itself async (like flatMap) — avoids CompletableFuture<CompletableFuture<T>>.
CompletableFuture<List<Order>> orders = user.thenCompose(u -> orderClient.recentAsync(u.id()));

// thenCombine: join two independent futures when both finish.
CompletableFuture<Quote> quote = priceF.thenCombine(taxF, (price, tax) -> new Quote(price, tax));

// thenAccept / thenRun: side effects with no result.
quote.thenAccept(q -> audit.log(q));

// anyOf: first to finish wins (e.g. hedged requests to two replicas). Result type is Object.
CompletableFuture<Object> fastest = CompletableFuture.anyOf(replicaA(), replicaB());

// *Async variants run the callback on the given executor instead of the completing thread.
user.thenApplyAsync(u -> slowEnrichment(u), io);
