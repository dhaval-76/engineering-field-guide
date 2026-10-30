# Python decorators are the everyday form of "advice around a method"
import functools, time, logging

def timed(fn):
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return fn(*args, **kwargs)
        finally:
            logging.info("%s took %.1f ms", fn.__name__, (time.perf_counter() - start) * 1000)
    return wrapper

class OrderService:
    @timed
    def place_order(self, req): ...
