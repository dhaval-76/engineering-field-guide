@Service
class OrderService {
    public void placeOrder(Cart cart) {
        validate(cart);
        saveOrder(cart);            // ✗ this.saveOrder(): bypasses the proxy → NO transaction
    }

    @Transactional
    public void saveOrder(Cart cart) { … }
}

// Same trap for @Async (runs synchronously), @Cacheable (never cached), @Retryable, @PreAuthorize.
// Also not advised: private, final or static methods, and calls during @PostConstruct.

// ✓ Fix 1 (best): put the advised method in another bean, so the call goes through its proxy
@Service
class OrderService {
    private final OrderWriter writer;
    OrderService(OrderWriter writer) { this.writer = writer; }
    public void placeOrder(Cart cart) { validate(cart); writer.saveOrder(cart); }
}
@Service
class OrderWriter {
    @Transactional public void saveOrder(Cart cart) { … }
}

// ✓ Fix 2: make the transaction boundary the public entry point
@Transactional
public void placeOrder(Cart cart) { … }

// ✓ Fix 3: programmatic transaction where the boundary must be inside one method
transactionTemplate.executeWithoutResult(status -> saveOrder(cart));
