from dataclasses import dataclass

@dataclass(frozen=True)
class Rendition:
    name: str
    kbps: int

class AbrController:
    """Adaptive bitrate selection, run before each segment download."""

    def __init__(self, ladder: list[Rendition]):
        self.ladder = ladder              # ascending bitrate
        self._estimate = -1.0             # kbps, exponentially weighted
        self._current = 0

    def on_segment_downloaded(self, bits: int, seconds: float) -> None:
        sample = bits / 1000 / seconds
        self._estimate = sample if self._estimate < 0 else 0.7 * self._estimate + 0.3 * sample

    def next(self, buffer_seconds: float) -> Rendition:
        if self._estimate < 0 or buffer_seconds < 5:          # start low; about to stall → safest
            self._current = 0
            return self.ladder[0]
        budget = 0.8 * self._estimate                          # 20% headroom
        best = max((i for i, r in enumerate(self.ladder) if r.kbps <= budget), default=0)
        if best > self._current:                               # climb one rung, only with buffer to spare
            best = self._current + 1 if buffer_seconds > 15 else self._current
        self._current = best
        return self.ladder[best]
