@Component
class StepWorker {
    private final RunStore runs;
    // per-connector rate limiting and retries inside
    private final Connectors connectors;
    private final StepQueue queue;
    private final TransactionTemplate tx;

    @KafkaListener(topics = "step-tasks", groupId = "engine")
    void execute(StepTask task) {
        Run run = runs.load(task.runId());
        Step step = run.definition().step(task.stepId());

        // redelivered after a crash: skip
        if (runs.isDone(run.id(), step.id())) {
            // idempotent: same task ids
            queue.enqueueNext(run, step);
            return;
        }
        if (step.condition() != null && !Expressions.evaluate(step.condition(), run.context())) {
            tx.executeWithoutResult(s -> { runs.recordSkipped(run.id(), step.id()); runs.outboxNext(run, step); });
            return;
        }

        // safe templating
        Map<String, Object> input = Templates.render(step.input(), run.context());
        StepOutcome outcome = connectors.get(step.type()).call(
            step.connection(), input, /* idempotency key */ run.id() + ":" + step.id());

        tx.executeWithoutResult(s -> {
            switch (outcome) {
                case StepOutcome.Done d -> {
                    runs.recordDone(run.id(), step.id(), d.output());
                    // next step task(s) in the SAME transaction
                    runs.outboxNext(run, step);
                }
                // e.g. 429 Retry-After
                case StepOutcome.RetryLater r -> runs.park(run.id(), step.id(), r.wakeAt());
                // owner notified
                case StepOutcome.Failed f -> runs.fail(run.id(), step.id(), f.error());
            }
        });
    }
}
