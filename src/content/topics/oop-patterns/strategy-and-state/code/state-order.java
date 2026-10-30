// Each state is an object that knows which actions are legal and what comes next.
sealed interface OrderState permits Placed, Paid, Shipped, Delivered, Cancelled {
    default OrderState pay()     { throw illegal("pay"); }
    default OrderState ship()    { throw illegal("ship"); }
    default OrderState deliver() { throw illegal("deliver"); }
    default OrderState cancel()  { throw illegal("cancel"); }
    private IllegalStateException illegal(String action) {
        return new IllegalStateException("cannot " + action + " when " + getClass().getSimpleName());
    }
}
record Placed()    implements OrderState {
    public OrderState pay()    { return new Paid(); }
    public OrderState cancel() { return new Cancelled(); }
}
record Paid()      implements OrderState {
    public OrderState ship()   { return new Shipped(); }
    public OrderState cancel() { return new Cancelled(); }     // refund handled by a listener
}
record Shipped()   implements OrderState { public OrderState deliver() { return new Delivered(); } }
record Delivered() implements OrderState {}
record Cancelled() implements OrderState {}

final class Order {                                            // the context
    private OrderState state = new Placed();
    void pay()     { state = state.pay(); }
    void ship()    { state = state.ship(); }
    void deliver() { state = state.deliver(); }
    void cancel()  { state = state.cancel(); }
    OrderState state() { return state; }
}
