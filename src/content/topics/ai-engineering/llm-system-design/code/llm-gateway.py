import threading
from dataclasses import dataclass
from typing import Protocol

@dataclass(frozen=True)
class Prompt:
    tenant: str
    text: str
    max_tokens: int

@dataclass(frozen=True)
class Completion:
    text: str
    input_tokens: int
    output_tokens: int
    provider: str

class LlmProvider(Protocol):
    name: str
    def complete(self, prompt: Prompt, timeout_s: float) -> Completion: ...

class BudgetExceeded(Exception):
    pass

class LlmGateway:
    """Per-tenant token budget per minute, plus a fallback chain of providers."""

    def __init__(self, chain: list[LlmProvider], tokens_per_minute: int):
        self.chain = chain
        self.tokens_per_minute = tokens_per_minute
        self.usage: dict[str, tuple[int, int]] = {}   # tenant -> (minute, tokens)
        self.lock = threading.Lock()

    def complete(self, p: Prompt, now_ms: int) -> Completion:
        estimate = len(p.text) // 4 + p.max_tokens
        self._reserve(p.tenant, estimate, now_ms)
        errors = []
        for provider in self.chain:
            try:
                c = provider.complete(p, timeout_s=20)
                self._adjust(p.tenant, c.input_tokens + c.output_tokens - estimate, now_ms)
                return c
            except Exception as e:          # try the next provider
                errors.append(f"{provider.name}: {e}")
        self._adjust(p.tenant, -estimate, now_ms)
        raise RuntimeError(f"all providers failed {errors}")

    def _reserve(self, tenant: str, tokens: int, now_ms: int) -> None:
        minute = now_ms // 60_000
        with self.lock:
            m, used = self.usage.get(tenant, (minute, 0))
            used = used if m == minute else 0
            if used + tokens > self.tokens_per_minute:
                raise BudgetExceeded("429 token budget exceeded")
            self.usage[tenant] = (minute, used + tokens)

    def _adjust(self, tenant: str, delta: int, now_ms: int) -> None:
        with self.lock:
            m, used = self.usage.get(tenant, (None, 0))
            if m == now_ms // 60_000:
                self.usage[tenant] = (m, max(0, used + delta))

    def used(self, tenant: str) -> int:
        return self.usage.get(tenant, (0, 0))[1]
