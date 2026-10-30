from typing import Callable

ShippingPolicy = Callable[["Order"], float]

SHIPPING: dict[str, ShippingPolicy] = {
    "STANDARD": lambda o: o.weight_kg * 40,
    "EXPRESS": lambda o: o.weight_kg * 90 + 50,
    "PICKUP": lambda o: 0.0,
}


def total(order, option: str) -> float:
    return order.subtotal + SHIPPING[option](order)
