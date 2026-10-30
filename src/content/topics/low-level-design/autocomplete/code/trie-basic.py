class Trie:
    def __init__(self):
        self.root: dict = {}

    def insert(self, word: str) -> None:
        node = self.root
        for ch in word:
            node = node.setdefault(ch, {})
        node["$"] = True  # end-of-word marker

    def complete(self, prefix: str, limit: int = 10) -> list[str]:
        node = self.root
        for ch in prefix:
            if ch not in node:
                return []
            node = node[ch]
        out: list[str] = []

        def walk(n: dict, acc: str) -> None:
            if len(out) >= limit:
                return
            if "$" in n:
                out.append(acc)
            for ch in sorted(k for k in n if k != "$"):
                walk(n[ch], acc + ch)

        walk(node, prefix)
        return out
