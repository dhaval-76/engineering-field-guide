# A registry of strategies: adding a discount registers a function; price() never changes.
_discounts = {
    "FLAT": lambda order: order["total"] - 10_000,           # paise
    "PERCENT": lambda order: order["total"] * 90 // 100,
}

def register_discount(name: str, fn) -> None:
    _discounts[name] = fn

def price(order: dict, discount_type: str) -> int:
    try:
        return _discounts[discount_type](order)
    except KeyError:
        raise ValueError(f"unknown discount {discount_type}") from None

register_discount("LOYALTY", lambda order: order["total"] * 95 // 100)
