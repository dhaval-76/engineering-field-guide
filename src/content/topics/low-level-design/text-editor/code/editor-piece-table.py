from dataclasses import dataclass

@dataclass(frozen=True)
class _Piece:
    from_add: bool
    start: int
    length: int

class PieceTable:
    """The original text is never modified; inserts append to an "add" buffer; the document is a list
    of pieces pointing into either buffer, so edits only split, drop or insert pieces."""

    def __init__(self, original: str = ""):
        self._original, self._add = original, ""
        self._pieces: list[_Piece] = [_Piece(False, 0, len(original))] if original else []

    def _buf(self, p: _Piece) -> str:
        return self._add if p.from_add else self._original

    def insert(self, pos: int, text: str) -> None:
        inserted = _Piece(True, len(self._add), len(text))
        self._add += text                            # append-only: old pieces stay valid
        offset = 0
        for i, p in enumerate(self._pieces):
            if pos <= offset + p.length:
                split = pos - offset                 # [left][inserted][right]
                parts = ([_Piece(p.from_add, p.start, split)] if split > 0 else []) + [inserted] + \
                        ([_Piece(p.from_add, p.start + split, p.length - split)] if split < p.length else [])
                self._pieces[i:i + 1] = parts
                return
            offset += p.length
        self._pieces.append(inserted)

    def delete(self, pos: int, length: int) -> str:
        """Keep the parts of each piece outside [pos, pos + length); return the removed text (for undo)."""
        removed, kept, offset, end = [], [], 0, pos + length
        for p in self._pieces:
            p_start, p_end = offset, offset + p.length
            offset = p_end
            lo, hi = max(pos, p_start), min(end, p_end)
            if lo >= hi:
                kept.append(p)
                continue
            removed.append(self._buf(p)[p.start + lo - p_start:p.start + hi - p_start])
            if lo > p_start:
                kept.append(_Piece(p.from_add, p.start, lo - p_start))
            if hi < p_end:
                kept.append(_Piece(p.from_add, p.start + hi - p_start, p_end - hi))
        self._pieces = kept
        return "".join(removed)

    def text(self) -> str:
        return "".join(self._buf(p)[p.start:p.start + p.length] for p in self._pieces)
