// A step worker: idempotent per (run, step), side effects keyed by the same id, next steps via the outbox.
export async function executeStep(task, { runs, connectors, inTx }) {
  const run = await runs.load(task.runId);
  const step = run.definition.steps[task.stepId];

  if (await runs.isDone(run.id, step.id)) {                    // redelivered after a crash: skip
    await runs.enqueueNext(run, step);                         // idempotent: same task ids
    return;
  }
  if (step.condition && !evaluate(step.condition, run.context)) {
    await inTx(async (tx) => { await runs.recordSkipped(tx, run.id, step.id); await runs.outboxNext(tx, run, step); });
    return;
  }

  const input = render(step.input, run.context);              // safe templating, no eval
  const outcome = await connectors[step.type].call(step.connection, input, { idempotencyKey: `${run.id}:${step.id}` });

  await inTx(async (tx) => {
    if (outcome.kind === 'done') {
      await runs.recordDone(tx, run.id, step.id, outcome.output);
      await runs.outboxNext(tx, run, step);                    // next step task(s) in the SAME transaction
    } else if (outcome.kind === 'retryLater') {
      await runs.park(tx, run.id, step.id, outcome.wakeAt);    // e.g. 429 Retry-After
    } else {
      await runs.fail(tx, run.id, step.id, outcome.error);     // owner notified
    }
  });
}
