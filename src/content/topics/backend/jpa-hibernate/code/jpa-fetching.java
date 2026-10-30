public interface OrderRepository extends JpaRepository<Order, Long> {

    // ✗ N+1: findAll() then order.getLines() in a loop → 1 + N SELECTs

    // ✓ 1. Fetch join: orders and their lines in one statement.
    //    DISTINCT is no longer needed for de-duplication since Hibernate 6.
    @Query("select o from Order o join fetch o.lines where o.customer.id = :customerId")
    List<Order> findWithLines(long customerId);

    // ✓ 2. Same thing, declaratively, on a derived query.
    @EntityGraph(attributePaths = {"lines", "customer"})
    List<Order> findByStatus(Status status);

    // ✓ 3. DTO projection: only the columns the screen needs, nothing managed.
    @Query("""
        select new com.shop.OrderSummary(o.id, c.name, o.status, count(l))
        from Order o join o.customer c left join o.lines l
        group by o.id, c.name, o.status""")
    List<OrderSummary> summaries();

    // ✓ 4. Paginating parents that have collections: page the ids, then fetch by ids.
    @Query("select o.id from Order o where o.status = :status")
    Page<Long> pageIds(Status status, Pageable pageable);

    @EntityGraph(attributePaths = "lines")
    List<Order> findByIdIn(Collection<Long> ids);
}

// ✓ 5. Batch fetching, globally (application.yml):
//    spring.jpa.properties.hibernate.default_batch_fetch_size: 50
//    → touching lines on 100 orders loads them with WHERE order_id IN (…50 ids…) twice.
