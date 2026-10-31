class PaymentDeclined(Exception):
    pass

class CheckoutFacade:
    """Many subsystems, each with its own API and failure modes, behind one entry point."""

    def __init__(self, carts, pricing, inventory, payments, orders):
        self.carts, self.pricing, self.inventory = carts, pricing, inventory
        self.payments, self.orders = payments, orders

    def checkout(self, cart_id: str, payment_details: dict) -> dict:
        cart = self.carts.load(cart_id)
        quote = self.pricing.quote(cart)
        hold = self.inventory.reserve(cart.items)
        try:
            paid = self.payments.pay(amount=quote.total, **payment_details)
            if not paid.succeeded:
                raise PaymentDeclined(paid.reason)
            order = self.orders.save(cart, quote, paid)
            self.inventory.commit(hold)
            return {"order_id": order.id, "total": quote.total}
        except Exception:
            self.inventory.release(hold)           # the facade owns the clean-up rules
            raise
