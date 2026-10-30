// ✗ N+1: one query for orders, then one per order for its lazily loaded items
List<Order> orders = orderRepo.findByCustomerId(customerId);   // 1 query
for (Order o : orders) total += o.getItems().size();            // +1 query per order → 501 queries for 500 orders

// ✓ Fetch join: one query (watch out: with pagination Hibernate paginates in memory — warning HHH90003004)
@Query("select distinct o from Order o join fetch o.items where o.customerId = :customerId")
List<Order> findWithItems(String customerId);

// ✓ Entity graph: same idea, declared on the repository method
@EntityGraph(attributePaths = {"items", "items.product"})
List<Order> findByCustomerId(String customerId);

// ✓ Batch fetching: N extra queries become N/50 — works with pagination
//   spring.jpa.properties.hibernate.default_batch_fetch_size: 50   (or @BatchSize(size = 50) on the collection)

// ✓ Often best for read endpoints: a DTO projection with exactly the columns the response needs
@Query("""
    select new com.shop.OrderSummary(o.id, o.createdAt, count(i), sum(i.price * i.quantity))
    from Order o join o.items i where o.customerId = :customerId group by o.id, o.createdAt""")
List<OrderSummary> summaries(String customerId);

// Catch it in tests: assert the statement count per request (Hibernate statistics or datasource-proxy)
