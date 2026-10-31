/** Composite: files and directories share one type, so traversal code treats them uniformly. */
sealed abstract class Node permits FileNode, DirNode {
    final String name;
    DirNode parent;
    Instant modified = Instant.now();

    Node(String name) { this.name = name; }   // validated when added to a directory
    abstract long size();
    String path() { return parent == null ? "/" : (parent.parent == null ? "" : parent.path()) + "/" + name; }
}

final class FileNode extends Node {
    private byte[] content = new byte[0];
    FileNode(String name) { super(name); }
    long size() { return content.length; }
    void write(byte[] data) { content = data.clone(); modified = Instant.now(); }
    void append(byte[] data) { byte[] n = Arrays.copyOf(content, content.length + data.length); System.arraycopy(data, 0, n, content.length, data.length); content = n; modified = Instant.now(); }
}

final class DirNode extends Node {
    // sorted names for `ls`
    private final Map<String, Node> children = new TreeMap<>();
    DirNode(String name) { super(name); }
    // the only nameless node
    static DirNode root() { return new DirNode(""); }
    // recursive by composition
    long size() { return children.values().stream().mapToLong(Node::size).sum(); }
    Optional<Node> child(String n) { return Optional.ofNullable(children.get(n)); }
    Collection<Node> children() { return Collections.unmodifiableCollection(children.values()); }
    void add(Node n) {
        if (n.name.isEmpty() || n.name.contains("/") || n.name.equals(".") || n.name.equals(".."))
            throw new IllegalArgumentException("bad name: " + n.name);
        if (children.putIfAbsent(n.name, n) != null) throw new IllegalStateException(n.name + " already exists");
        n.parent = this;
    }
    void remove(String n) { children.remove(n); }
}
