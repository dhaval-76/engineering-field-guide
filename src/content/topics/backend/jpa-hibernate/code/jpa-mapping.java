@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "order_seq", allocationSize = 50)
    private Long id;

    @Version
    private int version;                               // optimistic locking

    @ManyToOne(fetch = FetchType.LAZY, optional = false)  // default would be EAGER
    @JoinColumn(name = "customer_id")
    private Customer customer;                         // no cascade: a different aggregate

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderLine> lines = new HashSet<>();    // inverse side; LAZY by default

    @Enumerated(EnumType.STRING)                       // ORDINAL breaks when someone reorders the enum
    private Status status = Status.NEW;

    protected Order() {}                               // JPA needs a no-arg constructor
    public Order(Customer customer) { this.customer = customer; }

    // Keep both sides in sync — only OrderLine.order is written to the database.
    public void addLine(String sku, int qty) {
        lines.add(new OrderLine(this, sku, qty));
    }
    public void removeLine(OrderLine line) {
        lines.remove(line);                            // orphanRemoval → DELETE
        line.detachFromOrder();
    }

    // Stable across transient → managed: equal only when both have the same id.
    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Order other && id != null && id.equals(other.id));
    }
    @Override public int hashCode() { return Order.class.hashCode(); }
}

@Entity
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // the OWNING side: holds order_id
    @JoinColumn(name = "order_id")
    private Order order;

    private String sku;
    private int qty;

    protected OrderLine() {}
    OrderLine(Order order, String sku, int qty) { this.order = order; this.sku = sku; this.qty = qty; }
    void detachFromOrder() { this.order = null; }
}
