class Editor:
    """Commands know how to execute and undo themselves; two stacks give undo/redo."""

    def __init__(self, buffer):
        self.buffer = buffer
        self._undo, self._redo = [], []

    def apply(self, cmd) -> None:
        cmd.execute(self.buffer)
        self._undo.append(cmd)
        self._redo.clear()                  # a new edit invalidates the redo history

    def undo(self) -> bool:
        if not self._undo:
            return False
        cmd = self._undo.pop()
        cmd.undo(self.buffer)
        self._redo.append(cmd)
        return True

    def redo(self) -> bool:
        if not self._redo:
            return False
        cmd = self._redo.pop()
        cmd.execute(self.buffer)
        self._undo.append(cmd)
        return True

class InsertText:
    def __init__(self, pos: int, text: str):
        self.pos, self.text = pos, text
    def execute(self, b) -> None:
        b.insert(self.pos, self.text)
    def undo(self, b) -> None:
        b.delete(self.pos, len(self.text))

class DeleteText:
    def __init__(self, pos: int, length: int):
        self.pos, self.length, self.removed = pos, length, ""
    def execute(self, b) -> None:
        # remember what was removed for undo
        self.removed = b.delete(self.pos, self.length)
    def undo(self, b) -> None:
        b.insert(self.pos, self.removed)
