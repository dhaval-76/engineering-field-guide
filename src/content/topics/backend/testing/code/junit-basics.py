# pytest: plain functions and assert; fixtures give each test a fresh object.
import pytest
from shop.cart import ShoppingCart, UnknownSku, EmptyCart, bulk_percent

@pytest.fixture
def cart():
    return ShoppingCart({"BOOK": 1250, "PEN": 120})        # prices in paise

def test_total_sums_line_prices(cart):
    cart.add("BOOK", 2)                                    # arrange / act
    cart.add("PEN", 3)
    assert cart.total() == 2860                            # assert

def test_add_rejects_unknown_sku(cart):
    with pytest.raises(UnknownSku) as exc:
        cart.add("LAPTOP", 1)
    assert exc.value.sku == "LAPTOP"
    assert cart.is_empty()                                 # and nothing was half-added

@pytest.mark.parametrize("qty, expected", [(1, 0), (4, 0), (5, 10), (20, 10), (21, 15)])
def test_bulk_discount_by_quantity(qty, expected):
    assert bulk_percent(qty) == expected

class TestWhenEmpty:
    def test_total_is_zero(self, cart):
        assert cart.total() == 0

    def test_checkout_is_rejected(self, cart):
        with pytest.raises(EmptyCart):
            cart.checkout()
