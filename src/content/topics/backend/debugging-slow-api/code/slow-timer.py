# prometheus_client histogram + an OpenTelemetry span: the step shows up as its own metric
# and in traces.
from opentelemetry import trace
from prometheus_client import Histogram

BUILD_SECONDS = Histogram("dashboard_build_seconds", "Time to build the dashboard",
                          buckets=(0.05, 0.1, 0.25, 0.5, 1, 2, 5))
tracer = trace.get_tracer("dashboard")

def load_dashboard(user_id: str):
    with tracer.start_as_current_span("dashboard.load"), BUILD_SECONDS.time():
        return build(user_id)
# Reading the numbers: p50 fine but p99 bad → specific inputs or contention; p50 bad →
# everything is slow.
