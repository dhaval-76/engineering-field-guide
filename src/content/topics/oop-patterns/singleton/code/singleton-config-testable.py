import functools
import json

class FeatureFlags:
    def __init__(self, flags: dict[str, bool]):
        self._flags = dict(flags)

    def is_on(self, name: str) -> bool:
        return self._flags.get(name, False)

_override: FeatureFlags | None = None           # test seam

@functools.cache                                # loaded lazily, exactly once (thread-safe enough for reads)
def _load() -> FeatureFlags:
    with open("flags.json") as f:
        return FeatureFlags(json.load(f))

def feature_flags() -> FeatureFlags:
    return _override or _load()

def override_for_tests(flags: dict[str, bool]) -> None:
    global _override
    _override = FeatureFlags(flags)

def reset_for_tests() -> None:
    global _override
    _override = None
