class Node:
    def __init__(self, val, neighbors=None):
        self.val = val
        self.neighbors = neighbors or []

def clone_graph(node):
    copies = {}
    def dfs(n):
        if n in copies:
            return copies[n]
        copies[n] = Node(n.val)                  # register before recursing (cycles!)
        copies[n].neighbors = [dfs(nb) for nb in n.neighbors]
        return copies[n]
    return dfs(node) if node else None
