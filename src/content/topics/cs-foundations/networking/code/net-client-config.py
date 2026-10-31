# httpx: a shared Client keeps a connection pool; http2=True needs `pip install
# httpx[http2]`
import httpx

client = httpx.Client(
    http2=True,
    timeout=httpx.Timeout(2.0, connect=0.5),
    limits=httpx.Limits(max_connections=50, max_keepalive_connections=20, keepalive_expiry=30),
)

def fetch_cart(session_id: str) -> str:
    return client.get("https://shop.example.com/cart", cookies={"session": session_id}).text
# ✗ requests.get(...) per call without a Session opens a new connection each time.
