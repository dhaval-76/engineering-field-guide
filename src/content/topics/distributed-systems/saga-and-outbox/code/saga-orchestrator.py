from typing import Any, Protocol

class SagaStep(Protocol):
    name: str
    # idempotent local transaction
    def execute(self, ctx: dict[str, Any]) -> None: ...
    def compensate(self, ctx: dict[str, Any]) -> None: ...   # idempotent semantic undo

class SagaLog(Protocol):
    def record(self, saga_id: str, step: str, state: str) -> None: ...

def run_saga(saga_id: str, steps: list[SagaStep], ctx: dict, log: SagaLog,
             compensation_attempts: int = 5) -> str:
    done: list[SagaStep] = []
    for step in steps:
        log.record(saga_id, step.name, "STARTED")
        try:
            step.execute(ctx)
        except Exception:
            log.record(saga_id, step.name, "FAILED")
            return _compensate(saga_id, done, ctx, log, compensation_attempts)
        log.record(saga_id, step.name, "DONE")
        done.append(step)
    log.record(saga_id, "saga", "COMPLETED")
    return "COMPLETED"

def _compensate(saga_id, done, ctx, log, attempts) -> str:
    for step in reversed(done):                     # undo in reverse order
        ok = False
        for _ in range(attempts):
            try:
                step.compensate(ctx)
                ok = True
                break
            except Exception:
                pass                                # back off and retry
        log.record(saga_id, step.name, "COMPENSATED" if ok else "COMPENSATION_FAILED")
        if not ok:
            return "NEEDS_ATTENTION"                # page a human; don't guess
    log.record(saga_id, "saga", "COMPENSATED")
    return "COMPENSATED"
