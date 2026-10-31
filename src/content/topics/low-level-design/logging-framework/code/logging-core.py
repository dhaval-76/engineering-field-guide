"""A minimal logging framework (Python's own `logging` module follows the same design)."""
import contextvars
import sys
import threading
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import IntEnum
from typing import Callable, Protocol

class Level(IntEnum):
    DEBUG = 10
    INFO = 20
    WARN = 30
    ERROR = 40

# like an MDC
context: contextvars.ContextVar[dict] = contextvars.ContextVar("log_context", default={})

@dataclass(frozen=True)
class LogEvent:
    time: datetime
    level: Level
    logger: str
    thread: str
    message: str
    context: dict = field(default_factory=dict)

class Appender(Protocol):
    def append(self, e: LogEvent) -> None: ...

def pattern_formatter(e: LogEvent) -> str:
    ctx = f" {e.context}" if e.context else ""
    return f"{e.time.isoformat()} {e.level.name:<5} [{e.thread}] {e.logger} - {e.message}{ctx}"

class StreamAppender:
    def __init__(self, formatter: Callable[[LogEvent], str] = pattern_formatter, stream=sys.stdout):
        self.formatter, self.stream, self._lock = formatter, stream, threading.Lock()

    def append(self, e: LogEvent) -> None:
        with self._lock:                       # one line at a time, never interleaved
            print(self.formatter(e), file=self.stream)

class Logger:
    def __init__(self, name: str, parent: "Logger | None"):
        self.name, self.parent = name, parent
        self.level: Level | None = None        # None = inherit
        self.appenders: list[Appender] = []
        self.additive = True

    def effective_level(self) -> Level:
        node = self
        while node:
            if node.level is not None:
                return node.level
            node = node.parent
        return Level.INFO

    def log(self, level: Level, message: str | Callable[[], str]) -> None:
        if level < self.effective_level():
            return
        e = LogEvent(datetime.now(timezone.utc), level, self.name, threading.current_thread().name,
                     message() if callable(message) else message, dict(context.get()))
        node = self
        while node:
            for a in node.appenders:
                a.append(e)
            if not node.additive:
                break
            node = node.parent

    def debug(self, m): self.log(Level.DEBUG, m)
    def info(self, m): self.log(Level.INFO, m)
    def warn(self, m): self.log(Level.WARN, m)
    def error(self, m): self.log(Level.ERROR, m)

_root = Logger("root", None)
_loggers: dict[str, Logger] = {}
_registry_lock = threading.Lock()

def get_logger(name: str = "") -> Logger:
    if not name:
        return _root
    with _registry_lock:
        return _get(name)

def _get(name: str) -> Logger:
    if name not in _loggers:
        parent = _get(name.rsplit(".", 1)[0]) if "." in name else _root
        _loggers[name] = Logger(name, parent)
    return _loggers[name]
