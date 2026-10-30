# Python: tenacity for retries, httpx timeouts, pybreaker for a circuit breaker
import httpx, pybreaker
from tenacity import retry, stop_after_attempt, wait_random_exponential, retry_if_exception_type

inventory = httpx.Client(base_url="http://inventory", timeout=httpx.Timeout(0.8, connect=0.3))
breaker = pybreaker.CircuitBreaker(fail_max=5, reset_timeout=20)

class Retryable(Exception): ...

@breaker                                            # outermost here: open circuit → fail fast
@retry(
    stop=stop_after_attempt(3),
    wait=wait_random_exponential(multiplier=0.1, max=1),   # exponential backoff with jitter
    retry=retry_if_exception_type((httpx.TransportError, Retryable)),
    reraise=True,
)
def stock_for(sku: str) -> dict:
    r = inventory.get(f"/stock/{sku}")
    if r.status_code >= 500:
        raise Retryable(r.status_code)
    r.raise_for_status()
    return r.json()

def stock_or_fallback(sku: str) -> dict:
    try:
        return stock_for(sku)
    except (pybreaker.CircuitBreakerError, httpx.HTTPError, Retryable):
        return {"sku": sku, "status": "unknown"}     # degrade: the page still renders
