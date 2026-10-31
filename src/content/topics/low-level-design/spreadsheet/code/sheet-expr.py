import re
from dataclasses import dataclass
from typing import Callable

Get = Callable[[str], float]

@dataclass(frozen=True)
class Num:
    v: float
    def eval(self, get: Get) -> float: return self.v
    def references(self) -> set[str]: return set()

@dataclass(frozen=True)
class Ref:
    id: str
    def eval(self, get: Get) -> float: return get(self.id)
    def references(self) -> set[str]: return {self.id}

@dataclass(frozen=True)
class Bin:
    op: str
    l: object
    r: object
    def eval(self, get: Get) -> float:
        a, b = self.l.eval(get), self.r.eval(get)
        if self.op == "+": return a + b
        if self.op == "-": return a - b
        if self.op == "*": return a * b
        return a / b
    def references(self) -> set[str]: return self.l.references() | self.r.references()

@dataclass(frozen=True)
class Sum:
    cells: tuple
    def eval(self, get: Get) -> float: return sum(get(c) for c in self.cells)
    def references(self) -> set[str]: return set(self.cells)

def expand_range(a: str, b: str) -> tuple:
    """'A1', 'B3' → A1 A2 A3 B1 B2 B3 (single-letter columns for brevity)."""
    (c1, r1), (c2, r2) = (re.fullmatch(r"([A-Z])(\d+)", x).groups() for x in (a, b))
    return tuple(f"{chr(c)}{r}" for c in range(ord(min(c1, c2)), ord(max(c1, c2)) + 1)
                 for r in range(min(int(r1), int(r2)), max(int(r1), int(r2)) + 1))

class Parser:
    """Recursive descent: expr := term (('+'|'-') term)*, term := factor (('*'|'/') factor)*."""

    def __init__(self, s: str):
        self.s, self.i = s.replace(" ", ""), 0

    @classmethod
    def parse(cls, s: str):
        p = cls(s)
        e = p._expr()
        if p.i != len(p.s):
            raise p._error()
        return e

    def _expr(self):
        e = self._term()
        while self._peek("+") or self._peek("-"):
            op = self.s[self.i]; self.i += 1
            e = Bin(op, e, self._term())
        return e

    def _term(self):
        e = self._factor()
        while self._peek("*") or self._peek("/"):
            op = self.s[self.i]; self.i += 1
            e = Bin(op, e, self._factor())
        return e

    def _factor(self):
        if self._peek("("):
            self.i += 1
            e = self._expr()
            self._expect(")")
            return e
        if self.s.startswith("SUM(", self.i):
            self.i += 4
            a = self._ref(); self._expect(":"); b = self._ref(); self._expect(")")
            return Sum(expand_range(a, b))
        if self.i < len(self.s) and self.s[self.i].isalpha():
            return Ref(self._ref())
        m = re.compile(r"\d+(\.\d+)?").match(self.s, self.i)
        if not m:
            raise self._error()
        self.i = m.end()
        return Num(float(m.group()))

    def _ref(self) -> str:
        m = re.compile(r"[A-Z]+\d+").match(self.s, self.i)
        if not m:
            raise self._error()
        self.i = m.end()
        return m.group()

    def _peek(self, c: str) -> bool:
        return self.i < len(self.s) and self.s[self.i] == c

    def _expect(self, c: str) -> None:
        if not self._peek(c):
            raise self._error()
        self.i += 1

    def _error(self):
        return ValueError(f"bad formula near position {self.i}: {self.s}")
