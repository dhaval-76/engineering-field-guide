/** Replaces a file so that after a crash it holds the old contents or the new, never half. */
void writeDurably(Path target, byte[] data) throws IOException {
    Path temp = target.resolveSibling(target.getFileName() + ".tmp");
    try (FileChannel file = FileChannel.open(temp, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        while (buf.hasRemaining()) file.write(buf); // a write may be partial
        file.force(true); // fsync: until now the data may only be in the page cache
    }
    // rename is atomic: readers see the old file or the new one
    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    try (FileChannel dir = FileChannel.open(target.getParent(), StandardOpenOption.READ)) {
        dir.force(true); // fsync the directory, or the rename itself may be lost (Linux)
    }
}
