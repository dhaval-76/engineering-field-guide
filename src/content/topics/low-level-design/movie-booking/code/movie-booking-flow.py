# uses ShowSeat from above
import uuid
from datetime import datetime, timedelta, timezone

HOLD_TTL = timedelta(minutes=8)


class SeatUnavailable(Exception):
    pass


class PaymentFailed(Exception):
    pass


class HoldExpired(Exception):
    pass


class BookingService:
    def __init__(self, seats, payments, bookings, clock=lambda: datetime.now(timezone.utc)):
        self.seats, self.payments, self.bookings, self.clock = seats, payments, bookings, clock

    def hold(self, show_id: str, seat_ids: list[str], user_id: str) -> dict:
        """Step 1 — hold all requested seats or none (taken in sorted order)."""
        if not 1 <= len(seat_ids) <= 10:
            raise ValueError("1–10 seats")
        hold_id, taken = str(uuid.uuid4()), []
        for seat_id in sorted(seat_ids):
            s = self.seats.get(show_id, seat_id)
            if not s.try_hold(hold_id, self.clock(), HOLD_TTL):
                for t in taken:
                    t.release(hold_id)  # roll back the partial hold
                raise SeatUnavailable(seat_id)
            taken.append(s)
        return {
            "id": hold_id,
            "show_id": show_id,
            "user_id": user_id,
            "seat_ids": list(seat_ids),
            "expires_at": self.clock() + HOLD_TTL,
        }

    def checkout(self, hold: dict, amount: int, method) -> dict:
        """Step 2 — pay (idempotently, keyed by the hold), then confirm."""
        result = self.payments.charge(amount, method, idempotency_key=hold["id"])
        if not result.succeeded:
            self.release(hold)
            raise PaymentFailed(result.reason)
        if not all(
            self.seats.get(hold["show_id"], i).confirm(hold["id"], self.clock())
            for i in hold["seat_ids"]
        ):
            # expired while the user was paying
            self.payments.refund(result.payment_id, "hold expired")
            self.release(hold)
            raise HoldExpired(hold["id"])
        return self.bookings.save({**hold, "payment_id": result.payment_id, "status": "CONFIRMED"})

    def release(self, hold: dict) -> None:
        for i in hold["seat_ids"]:
            self.seats.get(hold["show_id"], i).release(hold["id"])
