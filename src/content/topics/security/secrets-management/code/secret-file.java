/**
 * Reads a secret from a file mounted by the platform (a Kubernetes secret, a Vault agent),
 * and picks up a rotated value without a restart. Never logs or returns it anywhere else.
 */
class FileSecret {
    private final Path path;
    private FileTime seen;
    private String value;

    FileSecret(Path path) {
        this.path = path;
    }

    synchronized String get() throws IOException {
        FileTime modified = Files.getLastModifiedTime(path);
        if (!modified.equals(seen)) { // the platform replaced the file: read the new value
            value = Files.readString(path).strip();
            seen = modified;
        }
        return value;
    }

    @Override
    public String toString() {
        return "FileSecret[" + path + ", value hidden]"; // safe if it ends up in a log
    }
}
