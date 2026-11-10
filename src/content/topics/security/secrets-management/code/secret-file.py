import os
import threading


class FileSecret:
    """Reads a secret from a file mounted by the platform (a Kubernetes secret, a Vault agent),
    and picks up a rotated value without a restart. Never logs or returns it anywhere else."""

    def __init__(self, path: str) -> None:
        self._path = path
        self._seen = -1
        self._value = ""
        self._lock = threading.Lock()

    def get(self) -> str:
        with self._lock:
            modified = os.stat(self._path).st_mtime_ns
            if modified != self._seen:  # the platform replaced the file: read the new value
                with open(self._path, encoding="utf-8") as f:
                    self._value = f.read().strip()
                self._seen = modified
            return self._value

    def __repr__(self) -> str:
        return f"FileSecret({self._path!r}, value hidden)"  # safe if it ends up in a log
