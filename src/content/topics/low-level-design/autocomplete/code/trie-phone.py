from dataclasses import dataclass, field

@dataclass(frozen=True)
class Contact:
    name: str
    number: str

@dataclass
class _Node:
    next: list = field(default_factory=lambda: [None] * 10)
    contacts: list = field(default_factory=list)

class PhoneDirectory:
    """Digits-only trie with contacts stored at the end node; prefix search returns contacts in digit order."""

    def __init__(self):
        self._root = _Node()

    def add(self, c: Contact) -> None:
        n = self._root
        for d in c.number:
            if not d.isdigit():
                raise ValueError(f"digits only: {c.number}")
            i = int(d)
            n.next[i] = n.next[i] or _Node()
            n = n.next[i]
        n.contacts.append(c)

    def by_prefix(self, digits: str, limit: int) -> list[Contact]:
        n = self._root
        for d in digits:
            n = n.next[int(d)]
            if n is None:
                return []
        out, stack = [], [n]
        while stack and len(out) < limit:
            cur = stack.pop()
            out.extend(cur.contacts)
            stack.extend(child for child in reversed(cur.next) if child)   # so 0 is visited first
        return out[:limit]
