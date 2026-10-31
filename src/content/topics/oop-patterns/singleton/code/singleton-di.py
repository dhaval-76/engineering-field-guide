from datetime import datetime, timezone

class InvoiceService:
    """Preferred in applications: create one instance at startup and pass it in."""

    def __init__(self, clock):
        self.clock = clock                      # dependency is visible in the constructor

    def issue(self, order) -> dict:
        return {"order": order, "issued_at": self.clock()}

# main.py — the only place that decides "one instance"
service = InvoiceService(clock=lambda: datetime.now(timezone.utc))

# In a test, pass a fixed clock — no global state to reset:
test_service = InvoiceService(clock=lambda: datetime(2026, 1, 1, tzinfo=timezone.utc))
# (A module is already a singleton in Python — a module-level instance works, but is harder to swap.)
