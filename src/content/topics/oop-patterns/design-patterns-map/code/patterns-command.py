from dataclasses import dataclass

@dataclass(frozen=True)
class InsertText:
    """Command: an action as an object — can be queued, logged, retried or undone."""
    doc: object
    pos: int
    text: str

    def execute(self) -> None:
        self.doc.insert(self.pos, self.text)

    def undo(self) -> None:
        self.doc.delete(self.pos, len(self.text))

class Editor:
    def __init__(self):
        self._undo, self._redo = [], []

    def run(self, cmd) -> None:
        cmd.execute()
        self._undo.append(cmd)
        self._redo.clear()

    def undo(self) -> None:
        if self._undo:
            cmd = self._undo.pop()
            cmd.undo()
            self._redo.append(cmd)

    def redo(self) -> None:
        if self._redo:
            cmd = self._redo.pop()
            cmd.execute()
            self._undo.append(cmd)
