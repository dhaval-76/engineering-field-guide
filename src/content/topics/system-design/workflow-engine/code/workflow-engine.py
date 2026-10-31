# A step worker: idempotent per (run, step), side effects keyed by the same id, next steps via the outbox.
def execute_step(task, runs, connectors, in_tx) -> None:
    run = runs.load(task.run_id)
    step = run.definition.steps[task.step_id]

    if runs.is_done(run.id, step.id):                        # redelivered after a crash: skip
        runs.enqueue_next(run, step)                         # idempotent: same task ids
        return
    if step.condition and not evaluate(step.condition, run.context):
        with in_tx() as tx:
            runs.record_skipped(tx, run.id, step.id)
            runs.outbox_next(tx, run, step)
        return

    inputs = render(step.input, run.context)                 # safe templating, no eval
    outcome = connectors[step.type].call(step.connection, inputs, idempotency_key=f"{run.id}:{step.id}")

    with in_tx() as tx:
        match outcome.kind:
            case "done":
                runs.record_done(tx, run.id, step.id, outcome.output)
                runs.outbox_next(tx, run, step)              # next step task(s) in the SAME transaction
            case "retry_later":
                runs.park(tx, run.id, step.id, outcome.wake_at)   # e.g. 429 Retry-After
            case _:
                runs.fail(tx, run.id, step.id, outcome.error)     # owner notified
