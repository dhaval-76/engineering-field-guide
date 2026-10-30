import org.testcontainers.postgresql.PostgreSQLContainer;   // Testcontainers 2.x package

@DataJpaTest                                              // JPA slice; each test rolls back
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // keep our DB, no H2
@Testcontainers
class OrderRepositoryTest {

    @Container
    @ServiceConnection                                    // Boot wires URL, user and password
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired OrderRepository repo;
    @Autowired TestEntityManager em;

    @Test
    void findWithLines_loadsLinesInOneQuery() {
        Customer c = em.persist(new Customer("Asha"));
        Order o = new Order(c);
        o.addLine("BOOK", 2);
        o.addLine("PEN", 1);
        em.persist(o);
        em.flush();
        em.clear();                                       // force a real read, not the 1st-level cache

        List<Order> found = repo.findWithLines(c.getId());

        assertThat(found).singleElement()
            .satisfies(order -> assertThat(order.getLines()).extracting(OrderLine::getSku)
                                                            .containsExactlyInAnyOrder("BOOK", "PEN"));
    }

    @Test
    void reserve_refusesWhenStockIsInsufficient() {
        Product p = em.persistAndFlush(new Product("BOOK", 1));
        assertThat(repo.reserve(p.getId(), 2)).isZero();  // the atomic UPDATE refuses
    }
}
