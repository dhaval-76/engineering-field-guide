// application.yml
//   spring.jpa.open-in-view: false     # lazy loading only inside transactions

@RestController
class OrderController {
    private final OrderService orders;

    @GetMapping("/orders/{id}")
    OrderDto get(@PathVariable long id) {
        return orders.details(id);            // a DTO: nothing lazy left to trigger
    }
}

@Service
class OrderService {
    private final OrderRepository repo;

    @Transactional(readOnly = true)
    public OrderDto details(long id) {
        // Fetch everything the response needs in one query, inside the transaction…
        Order o = repo.findDetailsById(id).orElseThrow(() -> new NotFoundException("order", id));
        // …and map while the persistence context is still open.
        return new OrderDto(o.getId(), o.getCustomer().getName(), o.getStatus(),
                o.getLines().stream().map(l -> new LineDto(l.getSku(), l.getQty())).toList());
    }
}

interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"customer", "lines"})
    Optional<Order> findDetailsById(long id);
}
