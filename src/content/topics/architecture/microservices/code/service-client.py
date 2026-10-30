# httpx with explicit timeouts (the default is 5 s for everything; be deliberate)
import httpx

payments = httpx.Client(
    base_url="http://payments",
    timeout=httpx.Timeout(2.0, connect=0.3),     # 300 ms to connect, 2 s for the rest
)

def charge(order_id: str, amount: str) -> dict:
    r = payments.post(
        "/api/payments",
        json={"orderId": order_id, "amount": amount},
        headers={"Idempotency-Key": f"order-{order_id}"},
    )
    r.raise_for_status()
    return r.json()
