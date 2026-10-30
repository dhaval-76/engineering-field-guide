import functools
import logging
import time

log = logging.getLogger(__name__)


def logged(fn):
    @functools.wraps(fn)
    def wrapper(payment):
        log.info("pay %s", payment.id)
        result = fn(payment)
        log.info("pay %s -> %s", payment.id, result.status)
        return result
    return wrapper


def retried(max_attempts=3):
    def decorate(fn):
        @functools.wraps(fn)
        def wrapper(payment):
            for attempt in range(1, max_attempts + 1):
                try:
                    return fn(payment)
                except TransientGatewayError:
                    if attempt == max_attempts:
                        raise
                    time.sleep(0.1 * 2**attempt)
        return wrapper
    return decorate


@logged
@retried(max_attempts=3)
def pay(payment): ...
