from dataclasses import dataclass

# Composite: a single item and a group of items answer the same question the same way.
@dataclass(frozen=True)
class FileLeaf:
    name: str
    size: int
    def size_bytes(self) -> int: return self.size

@dataclass(frozen=True)
class Folder:
    name: str
    children: tuple
    def size_bytes(self) -> int: return sum(c.size_bytes() for c in self.children)

# Folder("src", (FileLeaf("a.py", 1200), Folder("util", (FileLeaf("b.py",
# 300),)))).size_bytes()  → 1500
