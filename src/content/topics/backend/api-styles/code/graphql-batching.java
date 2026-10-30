// Spring for GraphQL: schema-first; method names match schema fields.
@Controller
class OrderGraphQlController {
    private final OrderRepository orders;
    private final CustomerRepository customers;

    OrderGraphQlController(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders; this.customers = customers;
    }

    @QueryMapping
    Order order(@Argument String id) {
        return orders.findById(id).orElse(null);
    }

    // Resolves Order.customer for ALL orders in the response with one query (no N+1).
    @BatchMapping
    Map<Order, Customer> customer(List<Order> orderList) {
        Set<String> ids = orderList.stream().map(Order::customerId).collect(Collectors.toSet());
        Map<String, Customer> byId = customers.findAllById(ids).stream()
                .collect(Collectors.toMap(Customer::id, c -> c));
        return orderList.stream().collect(Collectors.toMap(o -> o, o -> byId.get(o.customerId())));
    }

    @MutationMapping
    CancelOrderPayload cancelOrder(@Argument CancelOrderInput input, @AuthenticationPrincipal User user) {
        return orders.cancel(input.orderId(), user);   // authorization lives in the service, not the schema
    }
}
