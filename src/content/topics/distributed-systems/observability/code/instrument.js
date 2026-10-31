// OpenTelemetry API (SDK configured at startup) + pino JSON logs carrying the trace id.
import { trace, metrics, SpanStatusCode } from '@opentelemetry/api';
import pino from 'pino';

const tracer = trace.getTracer('checkout');
const meter = metrics.getMeter('checkout');
const ordersPlaced = meter.createCounter('orders.placed');
const duration = meter.createHistogram('checkout.duration', { unit: 'ms' });

const log = pino({
  mixin() {
    const span = trace.getActiveSpan();
    return span ? { trace_id: span.spanContext().traceId, span_id: span.spanContext().spanId } : {};
  },
});

export function checkout(cart, channel) {
  return tracer.startActiveSpan('checkout', async (span) => {
    const start = performance.now();
    let outcome = 'ok';
    span.setAttributes({ 'cart.id': cart.id, 'cart.items': cart.items.length, channel });
    try {
      const order = await placeOrder(cart);
      ordersPlaced.add(1, { channel });
      log.info({ orderId: order.id, totalCents: order.totalCents }, 'order placed');
      return order;
    } catch (e) {
      outcome = 'error';
      span.recordException(e);
      span.setStatus({ code: SpanStatusCode.ERROR });
      log.error({ err: e }, 'checkout failed');
      throw e;
    } finally {
      // low-cardinality labels only
      duration.record(performance.now() - start, { channel, outcome });
      span.end();
    }
  });
}
