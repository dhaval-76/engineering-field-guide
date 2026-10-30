# pytest + unittest.mock
from datetime import datetime, timezone
from unittest.mock import AsyncMock, Mock
import pytest

from shop.orders import OrderService, PaymentDeclined

REQUEST = {"customer_id": "C-1", "items": [{"sku": "BOOK", "qty": 2}]}
NOW = datetime(2026, 3, 1, 10, tzinfo=timezone.utc)

@pytest.fixture
def deps():
    orders = Mock()
    orders.save.side_effect = lambda order: order | {"id": "O-1"}   # return what was saved
    payments, events = AsyncMock(), Mock()
    return orders, payments, events

@pytest.fixture
def service(deps):
    orders, payments, events = deps
    return OrderService(orders, payments, events, clock=lambda: NOW)

async def test_charges_saves_and_publishes(service, deps):        # pytest-asyncio / anyio
    orders, payments, events = deps
    payments.charge.return_value = {"id": "ch_1", "status": "succeeded"}

    result = await service.place_order(REQUEST)

    assert result["status"] == "PAID"
    saved = orders.save.call_args.args[0]                          # like an ArgumentCaptor
    assert saved["customer_id"] == "C-1" and saved["placed_at"] == NOW
    events.publish.assert_called_once_with({"type": "OrderPlaced", "order_id": "O-1"})

async def test_declined_payment_saves_nothing(service, deps):
    orders, payments, events = deps
    payments.charge.side_effect = PaymentDeclined("card_declined")

    with pytest.raises(PaymentDeclined):
        await service.place_order(REQUEST)
    orders.save.assert_not_called()
    events.publish.assert_not_called()
