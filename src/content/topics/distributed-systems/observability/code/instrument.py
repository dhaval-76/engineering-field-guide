# OpenTelemetry API (SDK configured at startup) + JSON logs carrying the trace id.
import json, logging, time
from opentelemetry import metrics, trace
from opentelemetry.trace import Status, StatusCode

tracer = trace.get_tracer("checkout")
meter = metrics.get_meter("checkout")
orders_placed = meter.create_counter("orders.placed")
duration = meter.create_histogram("checkout.duration", unit="ms")

class JsonFormatter(logging.Formatter):
    def format(self, record):
        ctx = trace.get_current_span().get_span_context()
        entry = {"level": record.levelname, "msg": record.getMessage(), **getattr(record, "fields", {})}
        if ctx.is_valid:
            entry["trace_id"] = format(ctx.trace_id, "032x")
        return json.dumps(entry)

log = logging.getLogger("checkout")

def checkout(cart, channel: str):
    with tracer.start_as_current_span("checkout") as span:
        # high cardinality: span attribute, not a metric label
        span.set_attribute("cart.id", cart.id)
        span.set_attribute("channel", channel)
        start, outcome = time.perf_counter(), "ok"
        try:
            order = place_order(cart)
            orders_placed.add(1, {"channel": channel})
            log.info("order placed", extra={"fields": {"orderId": order.id, "totalCents": order.total_cents}})
            return order
        except Exception as e:
            outcome = "error"
            span.record_exception(e)
            span.set_status(Status(StatusCode.ERROR))
            raise
        finally:
            duration.record((time.perf_counter() - start) * 1000, {"channel": channel, "outcome": outcome})
