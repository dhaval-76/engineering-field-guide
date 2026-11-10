/** Sends a file to a socket. transferTo uses sendfile on Linux: no copy into the JVM. */
long sendFile(Path file, SocketChannel socket) throws IOException {
    try (FileChannel in = FileChannel.open(file, StandardOpenOption.READ)) {
        long sent = 0, size = in.size();
        while (sent < size) sent += in.transferTo(sent, size - sent, socket); // may send less
        return sent;
    }
}

/** The usual way, for comparison: every byte is copied into a buffer and back out. */
long sendFileCopying(Path file, SocketChannel socket) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
        byte[] buf = new byte[64 * 1024];
        long sent = 0;
        for (int n; (n = in.read(buf)) > 0; sent += n) {
            ByteBuffer chunk = ByteBuffer.wrap(buf, 0, n);
            while (chunk.hasRemaining()) socket.write(chunk);
        }
        return sent;
    }
}
