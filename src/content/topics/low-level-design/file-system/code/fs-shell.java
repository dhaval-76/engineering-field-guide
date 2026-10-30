final class FileSystem {
    private final DirNode root = DirNode.root();
    private DirNode cwd = root, previous = root;

    /** Resolve absolute or relative paths, handling "." and "..". */
    Node resolve(String path) {
        DirNode dir = path.startsWith("/") ? root : cwd;
        Node cur = dir;
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (!(cur instanceof DirNode d)) throw new NotADirectoryException(cur.path());
            cur = part.equals("..") ? (d.parent == null ? d : d.parent)
                                    : d.child(part).orElseThrow(() -> new NoSuchFileException(path));
        }
        return cur;
    }

    /** mkdir -p: create every missing directory along the path. */
    DirNode mkdirs(String path) {
        DirNode dir = path.startsWith("/") ? root : cwd;
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) { dir = dir.parent == null ? dir : dir.parent; continue; }
            Node next = dir.child(part).orElse(null);
            if (next == null) { next = new DirNode(part); dir.add(next); }
            if (!(next instanceof DirNode d)) throw new NotADirectoryException(next.path());
            dir = d;
        }
        return dir;
    }

    void cd(String path) {
        if (path.equals("-")) { DirNode t = cwd; cwd = previous; previous = t; return; }
        if (!(resolve(path) instanceof DirNode d)) throw new NotADirectoryException(path);
        previous = cwd;
        cwd = d;
    }

    String pwd() { return cwd.path(); }
    List<String> ls(String path) {
        Node n = resolve(path);
        return n instanceof DirNode d ? d.children().stream().map(c -> c.name + (c instanceof DirNode ? "/" : "")).toList()
                                      : List.of(n.name);
    }
}
