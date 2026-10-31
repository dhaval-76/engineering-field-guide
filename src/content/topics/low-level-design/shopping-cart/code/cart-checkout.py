from datetime import timedelta

class PaymentDeclined(Exception):
    pass

class CheckoutService:
    def __init__(self, carts, pricing, promotions, inventory, payments, orders):
        self.carts, self.pricing, self.promotions = carts, pricing, promotions
        self.inventory, self.payments, self.orders = inventory, payments, orders

    def checkout(self, checkout_id: str, cart_id: str, method):
        """`checkout_id` comes from the client and makes the operation idempotent (double-clicks, retries)."""
        if (existing := self.orders.find_by_checkout_id(checkout_id)) is not None:
            return existing
        cart = self.carts.get(cart_id)
        # re-price: never trust the client
        price = self.pricing.price(cart, self.promotions.active_for(cart))
        # all-or-nothing
        hold = self.inventory.reserve_all(cart.lines(), timedelta(minutes=15))
        try:
            paid = self.payments.charge(price["payable"], method, idempotency_key=checkout_id)
            if not paid.succeeded:
                raise PaymentDeclined(paid.reason)
            order = self.orders.save(checkout_id, cart, price, paid.reference)
            self.inventory.commit(hold)
            self.carts.clear(cart_id)
            return order
        except Exception:
            self.inventory.release(hold)
            raise
