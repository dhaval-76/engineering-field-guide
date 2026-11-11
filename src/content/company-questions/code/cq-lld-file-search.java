record FileEntry(String name, long sizeBytes, boolean directory, List<FileEntry> children) {
    String extension() {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}

/** Specification pattern: every condition is a filter, and filters combine into new ones. */
interface FileFilter {
    boolean matches(FileEntry f);

    default FileFilter and(FileFilter other) {
        return f -> matches(f) && other.matches(f);
    }

    default FileFilter or(FileFilter other) {
        return f -> matches(f) || other.matches(f);
    }

    default FileFilter negate() {
        return f -> !matches(f);
    }
}

final class Filters {
    static FileFilter nameContains(String part) {
        return f -> f.name().contains(part);
    }

    static FileFilter extension(String ext) {
        return f -> f.extension().equals(ext);
    }

    static FileFilter largerThan(long bytes) {
        return f -> f.sizeBytes() > bytes;
    }

    static FileFilter filesOnly() {
        return f -> !f.directory();
    }
}

/** Walking the tree is separate from matching, so new filters never touch this class. */
class FileSearcher {
    List<String> search(FileEntry root, FileFilter filter) {
        List<String> found = new ArrayList<>();
        Deque<Map.Entry<String, FileEntry>> stack = new ArrayDeque<>(); // iterative: deep trees
        stack.push(Map.entry(root.name(), root));
        while (!stack.isEmpty()) {
            var item = stack.pop();
            FileEntry entry = item.getValue();
            if (filter.matches(entry)) found.add(item.getKey());
            if (entry.directory()) {
                for (FileEntry child : entry.children()) {
                    stack.push(Map.entry(item.getKey() + "/" + child.name(), child));
                }
            }
        }
        Collections.sort(found);
        return found;
    }
}
