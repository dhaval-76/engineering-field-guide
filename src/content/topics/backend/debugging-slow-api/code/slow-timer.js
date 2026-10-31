// prom-client histogram + an OpenTelemetry span: the step shows up as its own metric and in traces.
import client from 'prom-client';
import { trace } from '@opentelemetry/api';

const buildSeconds = new client.Histogram({
  name: 'dashboard_build_seconds',
  help: 'Time to build the dashboard',
  buckets: [0.05, 0.1, 0.25, 0.5, 1, 2, 5],
});
const tracer = trace.getTracer('dashboard');

export async function loadDashboard(userId) {
  return tracer.startActiveSpan('dashboard.load', async (span) => {
    const end = buildSeconds.startTimer();
    try {
      return await build(userId);
    } finally {
      end();
      span.end();
    }
  });
}
// Reading the numbers: p50 fine but p99 bad → specific inputs or contention; p50 bad → everything is slow.
