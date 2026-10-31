import asyncio
from dataclasses import dataclass

@dataclass(frozen=True)
class PaymentResult:
    status: str                  # SUCCESS | PENDING | FAILED
    ref: str | None = None
    reason: str | None = None

class LegacyGatewayAdapter:
    """Adapts a callback-style legacy client (string amounts, one-letter statuses) to an async API."""

    def __init__(self, gateway):
        self.gateway = gateway

    async def pay(self, payment) -> PaymentResult:
        loop = asyncio.get_running_loop()
        done: asyncio.Future = loop.create_future()

        def callback(err, res):          # the legacy client may call back from another thread
            if err:
                loop.call_soon_threadsafe(done.set_exception, err)
            else:
                loop.call_soon_threadsafe(done.set_result, res)

        self.gateway.initiate_payment(
            {"txn_ref": payment.id, "amt": str(payment.amount_paise), "cur": payment.currency}, callback)
        res = await done
        if res["status"] == "S":
            return PaymentResult("SUCCESS", ref=res["gateway_ref"])
        if res["status"] == "P":
            return PaymentResult("PENDING", ref=res["gateway_ref"])
        return PaymentResult("FAILED", reason=res.get("err", "unknown"))
