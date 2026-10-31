# uses DirNode from the model above
class FileSystem:
    def __init__(self):
        self.root = DirNode.root()
        self.cwd = self.previous = self.root

    def resolve(self, path: str):
        """Resolve absolute or relative paths, handling "." and ".."."""
        cur = self.root if path.startswith("/") else self.cwd
        for part in path.split("/"):
            if part in ("", "."):
                continue
            if cur.type != "dir":
                raise NotADirectoryError(cur.path)
            nxt = (cur.parent or cur) if part == ".." else cur.child(part)
            if nxt is None:
                raise FileNotFoundError(path)
            cur = nxt
        return cur

    def mkdirs(self, path: str):
        """mkdir -p: create every missing directory along the path."""
        d = self.root if path.startswith("/") else self.cwd
        for part in path.split("/"):
            if part in ("", "."):
                continue
            if part == "..":
                d = d.parent or d
                continue
            nxt = d.child(part)
            if nxt is None:
                nxt = DirNode(part)
                d.add(nxt)
            if nxt.type != "dir":
                raise NotADirectoryError(nxt.path)
            d = nxt
        return d

    def cd(self, path: str) -> None:
        if path == "-":
            self.cwd, self.previous = self.previous, self.cwd
            return
        d = self.resolve(path)
        if d.type != "dir":
            raise NotADirectoryError(path)
        self.previous, self.cwd = self.cwd, d

    def pwd(self) -> str:
        return self.cwd.path

    def ls(self, path: str = ".") -> list[str]:
        n = self.resolve(path)
        return [c.name + ("/" if c.type == "dir" else "") for c in n.children] if n.type == "dir" else [n.name]
