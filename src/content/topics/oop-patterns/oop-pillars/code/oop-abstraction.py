from abc import ABC, abstractmethod


class PaymentProcessor(ABC):
    """The contract. Subclasses must implement both methods or they can't be instantiated."""

    @abstractmethod
    def charge(self, amount: int, customer_id: str) -> dict: ...

    @abstractmethod
    def refund(self, txn_id: str, amount: int) -> dict: ...


class CardProcessor(PaymentProcessor):
    def charge(self, amount, customer_id):
        return {"ok": True}  # call card gateway

    def refund(self, txn_id, amount):
        return {"ok": True}


class CheckoutService:
    def __init__(self, payments: PaymentProcessor):
        self._payments = payments

    def checkout(self, order) -> dict:
        return self._payments.charge(order.total, order.customer_id)
