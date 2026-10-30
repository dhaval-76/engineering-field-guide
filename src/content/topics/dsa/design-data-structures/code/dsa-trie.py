class Trie:
    def __init__(self):
        self.root = {}

    def insert(self, word):
        node = self.root
        for c in word:
            node = node.setdefault(c, {})
        node['$'] = True                       # end-of-word marker

    def _walk(self, s):
        node = self.root
        for c in s:
            node = node.get(c)
            if node is None:
                return None
        return node

    def search(self, word):
        node = self._walk(word)
        return node is not None and '$' in node

    def starts_with(self, prefix):
        return self._walk(prefix) is not None
