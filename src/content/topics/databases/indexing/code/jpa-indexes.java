@Entity
@Table(name = "orders", indexes = {
    // Documentation for the schema generator — real deployments create indexes in
    // Flyway/Liquibase migrations (CREATE INDEX CONCURRENTLY), where you can also add
    // DESC, INCLUDE, partial and expression indexes that JPA can't express.
    @Index(name = "orders_customer_status_created_idx", columnList = "customer_id, status, created_at DESC")
})
public class Order { /* … */ }

public interface UserRepository extends JpaRepository<User, Long> {

    // Generates: WHERE upper(u.email) = upper(?)  → needs an index on upper(email),
    // a plain index on email is ignored.
    Optional<User> findByEmailIgnoreCase(String email);

    // Generates: WHERE u.name LIKE ?  with '%' + name + '%'  → leading wildcard:
    // no B-tree can help; use a trigram index or full-text search.
    List<User> findByNameContaining(String name);

    // Fine with an index on (status, created_at): equality then sort, LIMIT 50.
    List<User> findTop50ByStatusOrderByCreatedAtDesc(Status status);
}
