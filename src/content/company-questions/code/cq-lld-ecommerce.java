record Product(String sku, String name, long pricePaise) {}

class Inventory {
    private final Map<String, AtomicInteger> available = new ConcurrentHashMap<>();

    void add(String sku, int qty) {
        available.computeIfAbsent(sku, s -> new AtomicInteger()).addAndGet(qty);
    }

    int available(String sku) {
        return available.getOrDefault(sku, new AtomicInteger()).get();
    }

    /** Atomic check-and-decrement: two buyers can never both take the last unit. */
    boolean reserve(String sku, int qty) {
        AtomicInteger stock = available.get(sku);
        if (stock == null) return false;
        while (true) {
            int now = stock.get();
            if (now < qty) return false;
            if (stock.compareAndSet(now, now - qty)) return true;
        }
    }

    void release(String sku, int qty) {
        available.get(sku).addAndGet(qty);
    }
}

class Cart {
    final Map<Product, Integer> items = new LinkedHashMap<>();

    void add(Product p, int qty) {
        items.merge(p, qty, Integer::sum);
    }

    long total() {
        return items.entrySet().stream()
                .mapToLong(e -> e.getKey().pricePaise() * e.getValue())
                .sum();
    }
}

enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    CANCELLED
}

class Order {
    final String id;
    final Map<Product, Integer> items;
    final long amount;
    OrderStatus status = OrderStatus.CREATED;

    Order(String id, Map<Product, Integer> items, long amount) {
        this.id = id;
        this.items = items;
        this.amount = amount;
    }
}

interface PaymentGateway {
    boolean charge(String orderId, long amountPaise);
}

class CheckoutService {
    private final Inventory inventory;
    private final PaymentGateway payments;
    private final AtomicLong ids = new AtomicLong();

    CheckoutService(Inventory inventory, PaymentGateway payments) {
        this.inventory = inventory;
        this.payments = payments;
    }

    /** All or nothing: reserve every line, pay, or put everything back. */
    Optional<Order> checkout(Cart cart) {
        Map<Product, Integer> reserved = new LinkedHashMap<>();
        for (var line : cart.items.entrySet()) {
            if (!inventory.reserve(line.getKey().sku(), line.getValue())) {
                reserved.forEach((p, q) -> inventory.release(p.sku(), q));
                return Optional.empty(); // out of stock
            }
            reserved.put(line.getKey(), line.getValue());
        }
        Order order = new Order("o" + ids.incrementAndGet(), Map.copyOf(reserved), cart.total());
        if (!payments.charge(order.id, order.amount)) {
            reserved.forEach((p, q) -> inventory.release(p.sku(), q));
            order.status = OrderStatus.CANCELLED;
            return Optional.of(order);
        }
        order.status = OrderStatus.PAID;
        return Optional.of(order);
    }

    void cancel(Order order) {
        if (order.status != OrderStatus.PAID) throw new IllegalStateException(order.status.name());
        order.items.forEach((p, q) -> inventory.release(p.sku(), q)); // refund goes here too
        order.status = OrderStatus.CANCELLED;
    }
}
