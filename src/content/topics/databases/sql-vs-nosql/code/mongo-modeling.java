// An order is an aggregate: always read and written as a whole → embed its items and address.
// {
//   "_id": "ord_8f2c",
//   "customerId": "cus_42",                       ← reference: customers live their own life
//   "status": "PLACED",
//   "items": [                                     ← embedded: bounded, read with the order
//     { "productId": "prd_1", "name": "Air 14", "qty": 1, "unitPrice": "999.00" },
//     { "productId": "prd_7", "name": "Sleeve", "qty": 1, "unitPrice": "29.00" }
//   ],
//   "shipping": { "line1": "12 MG Road", "city": "Pune", "postcode": "411001" },
//   "createdAt": ISODate("2026-03-01T10:00:00Z")
// }
// Product reviews are NOT embedded in products: unbounded, written independently → own collection.

@Document(collection = "orders")
@CompoundIndex(name = "customer_created", def = "{'customerId': 1, 'createdAt': -1}")
record Order(@Id String id, String customerId, Status status, List<OrderItem> items,
             Address shipping, Instant createdAt) {}

record OrderItem(String productId, String name, int qty, BigDecimal unitPrice) {}

interface OrderRepository extends MongoRepository<Order, String> {
    // Uses the compound index: equality on customerId, sorted by createdAt.
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable page);

    // Matches inside the embedded array; add an index on items.productId if this is hot.
    @Query("{ 'items.productId': ?0 }")
    List<Order> findContainingProduct(String productId);
}

// Atomic in-place update of one document — no read-modify-write in Java:
mongo.updateFirst(
    Query.query(Criteria.where("_id").is(orderId).and("status").is("PLACED")),
    new Update().set("status", "PAID"),
    Order.class);
