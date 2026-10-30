@RestController
@RequestMapping("/v1/orders")
class OrderController {
    private final OrderService orders;
    OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    ResponseEntity<OrderResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody CreateOrderRequest req) {
        Order order = orders.create(req, idempotencyKey);
        return ResponseEntity
            .created(URI.create("/v1/orders/" + order.id()))       // 201 + Location
            .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orders.find(id));                 // throws OrderNotFound → 404 via @ControllerAdvice
    }

    @PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
    OrderResponse update(@PathVariable String id, @RequestHeader("If-Match") String etag,
                         @RequestBody OrderPatch patch) {
        return OrderResponse.from(orders.patch(id, patch, etag));   // stale ETag → 412 Precondition Failed
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) { orders.delete(id); }
}

record CreateOrderRequest(
    @NotBlank String customerId,
    @NotEmpty List<@Valid LineItem> items,
    @Size(max = 500) String note) {}

record LineItem(@NotBlank String sku, @Positive int quantity) {}
