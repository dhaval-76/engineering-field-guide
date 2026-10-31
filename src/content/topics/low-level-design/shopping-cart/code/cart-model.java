record CartLine(String sku, int qty) {
    CartLine { if (qty <= 0) throw new IllegalArgumentException("qty must be > 0"); }
}

/**
 * The cart stores intent (what and how many), not prices — prices are looked up when
 * pricing.
 */
final class Cart {
    private final String cartId;
    private final String ownerId;                       // user id, or a guest session id
    private final Map<String, CartLine> lines = new LinkedHashMap<>();
    private final Set<String> couponCodes = new LinkedHashSet<>();
    // optimistic locking when persisted
    private long version;

    Cart(String cartId, String ownerId) { this.cartId = cartId; this.ownerId = ownerId; }

    void add(String sku, int qty) { lines.merge(sku, new CartLine(sku, qty), (a, b) -> new CartLine(sku, a.qty() + b.qty())); }
    void setQuantity(String sku, int qty) { if (qty == 0) lines.remove(sku); else lines.put(sku, new CartLine(sku, qty)); }
    void remove(String sku) { lines.remove(sku); }
    void applyCoupon(String code) { couponCodes.add(code.toUpperCase(Locale.ROOT)); }

    /** Guest cart merged into the user's cart at login: quantities add up. */
    void mergeFrom(Cart guest) { guest.lines.values().forEach(l -> add(l.sku(), l.qty())); couponCodes.addAll(guest.couponCodes); }

    List<CartLine> lines() { return List.copyOf(lines.values()); }
    Set<String> coupons() { return Set.copyOf(couponCodes); }
}
