from dataclasses import dataclass
from typing import Protocol

class Text(Protocol):
    def render(self) -> str: ...

@dataclass(frozen=True)
class Plain:
    value: str
    def render(self) -> str: return self.value

@dataclass(frozen=True)
class Bold:
    inner: Text
    def render(self) -> str: return f"<b>{self.inner.render()}</b>"

@dataclass(frozen=True)
class Italic:
    inner: Text
    def render(self) -> str: return f"<i>{self.inner.render()}</i>"

@dataclass(frozen=True)
class Underline:
    inner: Text
    def render(self) -> str: return f"<u>{self.inner.render()}</u>"

Underline(Italic(Bold(Plain("Hello")))).render()   # <u><i><b>Hello</b></i></u>
