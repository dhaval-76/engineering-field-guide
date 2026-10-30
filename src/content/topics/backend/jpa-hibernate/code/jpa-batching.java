// application.yml
//   spring.jpa.properties.hibernate.jdbc.batch_size: 50
//   spring.jpa.properties.hibernate.order_inserts: true     # group INSERTs by table
//   spring.jpa.properties.hibernate.order_updates: true
//   spring.datasource.url: jdbc:postgresql://db/shop?reWriteBatchedInserts=true

@Transactional
public void importProducts(Iterator<ProductRow> rows) {
    int i = 0;
    while (rows.hasNext()) {
        em.persist(Product.from(rows.next()));   // SEQUENCE ids: no round trip per row
        if (++i % 50 == 0) {
            em.flush();                          // send the batch of 50 INSERTs
            em.clear();                          // drop snapshots so memory stays flat
        }
    }
}

public interface ProductRepository extends JpaRepository<Product, Long> {
    // One statement instead of loading and dirty-checking every row.
    // It bypasses the persistence context: clear it so later reads aren't stale.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int reprice(String category, BigDecimal factor);
}
