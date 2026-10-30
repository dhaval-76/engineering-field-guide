// Time a specific step so it shows up as its own metric (and span, with @Observed)
@Service
class DashboardService {
    private final Timer buildTimer;

    DashboardService(MeterRegistry registry) {
        this.buildTimer = Timer.builder("dashboard.build")
            .publishPercentileHistogram()
            .register(registry);
    }

    @Observed(name = "dashboard.load")          // creates a timer and a tracing span (needs AOP + ObservedAspect bean)
    public Dashboard load(String userId) {
        return buildTimer.record(() -> build(userId));
    }
}
// Reading the numbers: p50 fine but p99 bad → specific inputs or contention; p50 bad → everything is slow.
