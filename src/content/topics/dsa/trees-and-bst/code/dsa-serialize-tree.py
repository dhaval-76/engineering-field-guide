# Pre-order with "#" for null children: the markers make the shape unambiguous, so one
# traversal is enough to rebuild the tree. [1,2,3,None,None,4,5] ↔ "1,2,#,#,3,4,#,#,5,#,#"
def serialize(root: "TreeNode | None") -> str:
    out: list[str] = []

    def write(n: "TreeNode | None") -> None:
        if n is None:
            out.append("#")
            return
        out.append(str(n.val))
        write(n.left)
        write(n.right)

    write(root)
    return ",".join(out)


def deserialize(data: str) -> "TreeNode | None":
    tokens = iter(data.split(","))

    # Reads tokens in the same pre-order they were written: node, left subtree, right
    # subtree.
    def read() -> "TreeNode | None":
        t = next(tokens)
        if t == "#":
            return None
        n = TreeNode(int(t))
        n.left = read()
        n.right = read()
        return n

    return read()
# O(n) both ways. Without null markers, pre-order alone can't distinguish shapes.
