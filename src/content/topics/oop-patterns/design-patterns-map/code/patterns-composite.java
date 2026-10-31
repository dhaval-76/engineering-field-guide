// Composite: treat a single item and a group of items through the same interface.
sealed interface FileNode permits FileLeaf, Folder { long sizeBytes(); }
record FileLeaf(String name, long sizeBytes) implements FileNode {}
record Folder(String name, List<FileNode> children) implements FileNode {
    public long sizeBytes() { return children.stream().mapToLong(FileNode::sizeBytes).sum(); }
}
// Folder("src", List.of(new FileLeaf("a.java", 1200), new Folder("util",
// List.of(...)))).sizeBytes();
