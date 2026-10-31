from dataclasses import dataclass, replace

@dataclass(frozen=True)
class Ins:
    pos: int
    text: str

@dataclass(frozen=True)
class Del:
    pos: int
    length: int

def transform(op, applied, op_wins_ties: bool):
    """Rewrite `op` so it applies after `applied` (both made against the same document)."""
    if isinstance(op, Ins):
        if isinstance(applied, Ins):
            shift = applied.pos < op.pos or (applied.pos == op.pos and not op_wins_ties)
            return replace(op, pos=op.pos + len(applied.text)) if shift else op
        if op.pos <= applied.pos:
            return op
        if op.pos >= applied.pos + applied.length:
            return replace(op, pos=op.pos - applied.length)
        # typed inside a deleted range: dropped (same rule both ways)
        return Ins(applied.pos, "")
    if isinstance(applied, Ins):
        if applied.pos <= op.pos:
            return replace(op, pos=op.pos + len(applied.text))
        if applied.pos >= op.pos + op.length:
            return op
        # delete the text inserted inside our range
        return replace(op, length=op.length + len(applied.text))
    s, e, ds, de = op.pos, op.pos + op.length, applied.pos, applied.pos + applied.length
    if e <= ds:
        return op
    if s >= de:
        return replace(op, pos=s - applied.length)
    overlap = min(e, de) - max(s, ds)
    return Del(min(s, ds), op.length - overlap)

def apply(doc: str, op) -> str:
    if isinstance(op, Ins):
        return doc[:op.pos] + op.text + doc[op.pos:]
    return doc[:op.pos] + doc[op.pos + op.length:]
