@Service
class OrderService {

    // Role check: who may call this at all
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(String id) { orders.deleteById(id); }

    // Ownership check: the data decides — compare the resource with the caller
    @PostAuthorize("returnObject.customerId() == authentication.name or hasRole('SUPPORT')")
    public OrderView getOrder(String id) { return orders.view(id); }

    // Better for lists and for hiding existence: scope the QUERY to the caller
    public List<OrderView> myOrders(Authentication auth, Cursor cursor) {
        return orders.findByCustomerId(auth.getName(), cursor);      // never findAll() then filter
    }
}
// Return 404 rather than 403 for "exists but not yours" if ids are guessable and existence is sensitive.
// Test both: no token → 401; valid token without permission → 403/404; someone else's id → 404.
