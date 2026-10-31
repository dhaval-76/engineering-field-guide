// Server: generated base class from api-orders.proto
class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {
    @Override
    public void getOrder(GetOrderRequest req, StreamObserver<Order> response) {
        repository.find(req.getOrderId()).ifPresentOrElse(
                order -> { response.onNext(order); response.onCompleted(); },
                () -> response.onError(Status.NOT_FOUND
                        .withDescription("order " + req.getOrderId()).asRuntimeException()));
    }
}

// Client: reuse one channel per target; always set a deadline per call.
ManagedChannel channel = ManagedChannelBuilder.forTarget("dns:///orders.internal:443")
        // spread calls across pods, not one HTTP/2 connection
        .defaultLoadBalancingPolicy("round_robin")
        .build();
OrderServiceGrpc.OrderServiceBlockingStub stub = OrderServiceGrpc.newBlockingStub(channel);

try {
    Order order = stub.withDeadlineAfter(300, TimeUnit.MILLISECONDS)
            .getOrder(GetOrderRequest.newBuilder().setOrderId("A-1001").build());
} catch (StatusRuntimeException e) {
    switch (e.getStatus().getCode()) {
        case NOT_FOUND -> { /* 404-equivalent */ }
        case DEADLINE_EXCEEDED, UNAVAILABLE -> { /* retry with backoff if idempotent */ }
        default -> throw e;
    }
}
