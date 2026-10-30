@Transactional
public Order placeOrder(long customerId, List<LineRequest> lines) {
    // We only need the customer as a foreign key. getReference() returns a proxy
    // holding just the id — no SELECT on customers at all.
    Customer customer = em.getReference(Customer.class, customerId);

    Order order = new Order(customer);       // order.customer_id = customerId
    lines.forEach(l -> order.addLine(l.sku(), l.qty()));
    em.persist(order);                       // INSERT INTO orders(customer_id, …) VALUES (42, …)
    return order;
    // If customer 42 doesn't exist, the FK constraint fails at flush.
    // Reading customer.getName() would trigger the SELECT (and EntityNotFoundException if missing).
}

@Transactional(readOnly = true)
public CustomerView show(long id) {
    Customer c = em.find(Customer.class, id);   // SELECT now; null if there's no such row
    if (c == null) throw new NotFoundException("customer", id);
    return CustomerView.from(c);
}

// Spring Data equivalents: repo.findById(id) → Optional (find), repo.getReferenceById(id) → proxy
