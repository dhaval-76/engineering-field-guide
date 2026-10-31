import random
import threading
import time

def transfer(src, dst, amount, budget_s: float = 1.0) -> bool:
    """Break "no preemption": if the second lock isn't available soon, release the first
    and retry."""
    deadline = time.monotonic() + budget_s
    while time.monotonic() < deadline:
        if src.lock.acquire(timeout=0.05):
            try:
                if dst.lock.acquire(timeout=0.05):
                    try:
                        src.balance -= amount
                        dst.balance += amount
                        return True
                    finally:
                        dst.lock.release()
            finally:
                src.lock.release()
        time.sleep(random.uniform(0.001, 0.02))     # RANDOM backoff — avoids livelock
    return False                                    # caller reports "busy, try again"
