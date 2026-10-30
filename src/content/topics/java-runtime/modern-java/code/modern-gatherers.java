// Stream gatherers (24): custom intermediate operations — e.g. fixed-size batches and sliding windows.
List<List<Order>> batches = orders.stream().gather(Gatherers.windowFixed(100)).toList();
List<List<Double>> moving = prices.stream().gather(Gatherers.windowSliding(3)).toList();

// Scoped values (25): immutable, inheritable per-request context — a safer alternative to ThreadLocal
static final ScopedValue<RequestContext> CTX = ScopedValue.newInstance();
ScopedValue.where(CTX, new RequestContext(userId)).run(() -> handle(request));   // CTX.get() inside handle()
