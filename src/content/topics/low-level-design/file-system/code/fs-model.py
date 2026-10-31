import time

class Node:
    """Composite: files and directories share one type, so traversal code treats them uniformly."""

    def __init__(self, name: str):
        self.name, self.parent, self.modified = name, None, time.time()

    @property
    def path(self) -> str:
        if self.parent is None:
            return "/"
        return (self.parent.path if self.parent.parent else "") + "/" + self.name

class FileNode(Node):
    type = "file"

    def __init__(self, name: str):
        super().__init__(name)
        self._content = b""

    @property
    def size(self) -> int:
        return len(self._content)

    def write(self, data: bytes) -> None:
        self._content, self.modified = bytes(data), time.time()

    def append(self, data: bytes) -> None:
        self._content, self.modified = self._content + data, time.time()

class DirNode(Node):
    type = "dir"

    def __init__(self, name: str):
        super().__init__(name)
        self._children: dict[str, Node] = {}

    @classmethod
    def root(cls) -> "DirNode":
        return cls("")                                     # the only nameless node

    @property
    def size(self) -> int:
        return sum(c.size for c in self._children.values())   # recursive by composition

    @property
    def children(self) -> list[Node]:
        return [self._children[k] for k in sorted(self._children)]   # sorted for ls

    def child(self, name: str) -> Node | None:
        return self._children.get(name)

    def add(self, node: Node) -> None:
        if not node.name or "/" in node.name or node.name in (".", ".."):
            raise ValueError(f"bad name: {node.name!r}")
        if node.name in self._children:
            raise FileExistsError(node.name)
        self._children[node.name] = node
        node.parent = self

    def remove(self, name: str) -> None:
        self._children.pop(name, None)
