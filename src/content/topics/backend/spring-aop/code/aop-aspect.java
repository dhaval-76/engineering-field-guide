@Target(METHOD) @Retention(RUNTIME)
public @interface Audited { String action(); }

@Aspect
@Component
class ObservabilityAspect {
    private final MeterRegistry metrics;
    private final AuditLog audit;

    ObservabilityAspect(MeterRegistry metrics, AuditLog audit) { this.metrics = metrics; this.audit = audit; }

    // Pointcut: every public method of beans in the service package
    @Pointcut("execution(public * com.shop.service..*(..))")
    void serviceMethods() {}

    // @Around: wraps the call — can measure, retry, change arguments or results, or skip the call
    @Around("serviceMethods()")
    Object time(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = Timer.start(metrics);
        String outcome = "success";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            outcome = t.getClass().getSimpleName();
            throw t;                                             // never swallow
        } finally {
            sample.stop(metrics.timer("service.calls",
                "method", pjp.getSignature().toShortString(), "outcome", outcome));
        }
    }

    // @AfterReturning on a custom annotation: audit only successful actions
    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    void audit(JoinPoint jp, Audited audited, Object result) {
        audit.record(audited.action(), SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

// Usage
@Service
class RefundService {
    @Audited(action = "REFUND_ISSUED")
    public Refund issue(String paymentId, Money amount) { … }
}
