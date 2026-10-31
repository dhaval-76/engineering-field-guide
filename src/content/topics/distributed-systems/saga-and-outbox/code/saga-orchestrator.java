interface SagaStep {
    String name();
    void execute(Map<String, Object> ctx) throws Exception;     // local transaction in one service (idempotent)
    void compensate(Map<String, Object> ctx) throws Exception;  // semantic undo (idempotent, retryable)
}

interface SagaLog { void record(String sagaId, String step, String state); }   // a table in production

enum SagaResult { COMPLETED, COMPENSATED, NEEDS_ATTENTION }

class SagaOrchestrator {
    private final SagaLog log;
    private final int compensationAttempts;

    SagaOrchestrator(SagaLog log, int compensationAttempts) {
        this.log = log; this.compensationAttempts = compensationAttempts;
    }

    SagaResult run(String sagaId, List<SagaStep> steps, Map<String, Object> ctx) {
        int done = 0;
        for (SagaStep step : steps) {
            log.record(sagaId, step.name(), "STARTED");
            try {
                step.execute(ctx);
                log.record(sagaId, step.name(), "DONE");
                done++;
            } catch (Exception e) {
                log.record(sagaId, step.name(), "FAILED");
                return compensate(sagaId, steps.subList(0, done), ctx);
            }
        }
        log.record(sagaId, "saga", "COMPLETED");
        return SagaResult.COMPLETED;
    }

    private SagaResult compensate(String sagaId, List<SagaStep> completed, Map<String, Object> ctx) {
        for (int i = completed.size() - 1; i >= 0; i--) {                 // undo in reverse order
            SagaStep step = completed.get(i);
            boolean ok = false;
            for (int attempt = 0; attempt < compensationAttempts && !ok; attempt++) {
                try { step.compensate(ctx); ok = true; }
                catch (Exception e) { /* back off and retry: compensations must eventually succeed */ }
            }
            log.record(sagaId, step.name(), ok ? "COMPENSATED" : "COMPENSATION_FAILED");
            if (!ok) return SagaResult.NEEDS_ATTENTION;                   // page a human; don't guess
        }
        log.record(sagaId, "saga", "COMPENSATED");
        return SagaResult.COMPENSATED;
    }
}
