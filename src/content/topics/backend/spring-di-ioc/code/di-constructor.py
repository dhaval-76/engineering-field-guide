class CheckoutService:
    def __init__(self, gateway: PaymentGateway, orders: OrderRepository) -> None:
        self.gateway = gateway
        self.orders = orders

# FastAPI's Depends is a small DI container: it builds dependencies per request
def get_checkout(db: Session = Depends(get_db)) -> CheckoutService:
    return CheckoutService(StripeGateway(settings.stripe_key), SqlOrderRepository(db))

@app.post("/checkout")
def checkout(cart: Cart, service: CheckoutService = Depends(get_checkout)):
    return service.place(cart)

# Tests: app.dependency_overrides[get_checkout] = lambda: CheckoutService(FakeGateway(),
# InMemoryOrders())
