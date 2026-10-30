from collections import deque

def preorder(n):
    return [n.val, *preorder(n.left), *preorder(n.right)] if n else []

def inorder(n):
    return [*inorder(n.left), n.val, *inorder(n.right)] if n else []

def postorder(n):
    return [*postorder(n.left), *postorder(n.right), n.val] if n else []

def level_order(root):
    levels, queue = [], deque([root] if root else [])
    while queue:
        level = []
        for _ in range(len(queue)):
            n = queue.popleft()
            level.append(n.val)
            queue.extend(c for c in (n.left, n.right) if c)
        levels.append(level)
    return levels
