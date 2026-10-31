// step: { name, execute(ctx), compensate(ctx) } — both idempotent; log.record persists progress.
export async function runSaga(sagaId, steps, ctx, log, compensationAttempts = 5) {
  const done = [];
  for (const step of steps) {
    await log.record(sagaId, step.name, 'STARTED');
    try {
      await step.execute(ctx);
      await log.record(sagaId, step.name, 'DONE');
      done.push(step);
    } catch {
      await log.record(sagaId, step.name, 'FAILED');
      return compensate(sagaId, done, ctx, log, compensationAttempts);
    }
  }
  await log.record(sagaId, 'saga', 'COMPLETED');
  return 'COMPLETED';
}

async function compensate(sagaId, done, ctx, log, attempts) {
  for (const step of [...done].reverse()) {
    let ok = false;
    for (let a = 0; a < attempts && !ok; a++) {
      try {
        await step.compensate(ctx);
        ok = true;
      } catch {
        /* back off and retry */
      }
    }
    await log.record(sagaId, step.name, ok ? 'COMPENSATED' : 'COMPENSATION_FAILED');
    if (!ok) return 'NEEDS_ATTENTION';
  }
  await log.record(sagaId, 'saga', 'COMPENSATED');
  return 'COMPENSATED';
}
