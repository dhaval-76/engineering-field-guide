import uuid
from datetime import datetime, timezone
import pytest

# 1. Time: production code receives a clock instead of calling datetime.now() directly.
def is_expired(trial, clock=lambda: datetime.now(timezone.utc)) -> bool:
    return clock() > trial.ends_at

def test_trial_expires_after_end_time():
    fixed = lambda: datetime(2026, 3, 15, 0, 0, 1, tzinfo=timezone.utc)
    trial = Trial(ends_at=datetime(2026, 3, 15, tzinfo=timezone.utc))
    assert is_expired(trial, clock=fixed)                     # same result every run
# (Or the freezegun / time-machine libraries for code you can't change.)

# 2. Async: wait for the condition, not for an arbitrary number of seconds.
def wait_until(condition, timeout=5.0, interval=0.05):
    import time
    deadline = time.monotonic() + timeout
    while not condition():
        if time.monotonic() > deadline:
            raise AssertionError("condition not met in time")
        time.sleep(interval)

def test_order_eventually_sends_confirmation(service, mailbox):
    service.place_order(REQUEST)
    wait_until(lambda: len(mailbox.sent_to("c1@example.com")) == 1)
    # ✗ time.sleep(0.5) — too short on a busy CI machine, wasted time everywhere else

# 3. Shared state: each test creates the data it needs, with unique keys.
def test_customer_can_have_only_one_active_cart(customers, carts):
    # no clash with other tests
    customer = customers.create(f"user-{uuid.uuid4()}@example.com")
    carts.open(customer)
    with pytest.raises(ActiveCartExists):
        carts.open(customer)
