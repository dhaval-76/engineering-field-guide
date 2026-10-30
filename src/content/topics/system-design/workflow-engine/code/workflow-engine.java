@Component
class StepWorker {
    private final RunStore runs;
    private final Connectors connectors;          // per-connector rate limiting and retries inside
    private final StepQueue queue;
    private final TransactionTemplate tx;

    @KafkaListener(topics = "step-tasks", groupId = "engine")
    void execute(StepTask task) {
        Run run = runs.load(task.runId());
        Step step = run.definition().step(task.stepId());

        if (runs.isDone(run.id(), step.id())) {                       // redelivered after a crash: skip
            queue.enqueueNext(run, step);                             // idempotent: same task ids
            return;
        }
        if (step.condition() != null && !Expressions.evaluate(step.condition(), run.context())) {
            tx.executeWithoutResult(s -> { runs.recordSkipped(run.id(), step.id()); runs.outboxNext(run, step); });
            return;
        }

        Map<String, Object> input = Templates.render(step.input(), run.context());   // safe templating
        StepOutcome outcome = connectors.get(step.type()).call(
            step.connection(), input, /* idempotency key */ run.id() + ":" + step.id());

        tx.executeWithoutResult(s -> {
            switch (outcome) {
                case StepOutcome.Done d -> {
                    runs.recordDone(run.id(), step.id(), d.output());
                    runs.outboxNext(run, step);                       // next step task(s) in the SAME transaction
                }
                case StepOutcome.RetryLater r -> runs.park(run.id(), step.id(), r.wakeAt());   // e.g. 429 Retry-After
                case StepOutcome.Failed f -> runs.fail(run.id(), step.id(), f.error());       // owner notified
            }
        });
    }
}
