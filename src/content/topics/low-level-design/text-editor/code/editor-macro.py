class CompositeCommand:
    """One undo step for many actions (paste-with-format, replace-all, macros)."""

    def __init__(self, steps):
        self.steps = list(steps)

    def execute(self, b) -> None:
        for s in self.steps:
            s.execute(b)

    def undo(self, b) -> None:
        for s in reversed(self.steps):          # reverse order
            s.undo(b)

class MacroRecorder:
    """Capture commands while recording; replay them later as one composite."""

    def __init__(self):
        self._recording = None

    def start(self) -> None:
        self._recording = []

    def capture(self, cmd) -> None:
        if self._recording is not None:
            self._recording.append(cmd)

    def stop(self) -> CompositeCommand:
        macro, self._recording = CompositeCommand(self._recording or []), None
        return macro
