from typing import Protocol

# ✗ One interface for every destination: log(), log_to_file(), log_to_db(),
# log_to_splunk()...

# ✓ One small contract; destinations are separate implementations (or composed sinks).
class LogSink(Protocol):
    def write(self, event: dict) -> None: ...

class ConsoleSink:
    def write(self, event: dict) -> None:
        print(event)

class FileSink:
    def __init__(self, path: str):
        self.path = path
    def write(self, event: dict) -> None:
        with open(self.path, "a") as f:
            f.write(f"{event}\n")

class Logger:
    def __init__(self, sinks: list[LogSink]):
        self.sinks = sinks
    def info(self, msg: str) -> None:
        event = {"level": "INFO", "msg": msg}
        for s in self.sinks:
            s.write(event)
