/** Prefixes each message with its length as 4 bytes, so the receiver knows where it ends. */
byte[] encode(String message) {
    byte[] body = message.getBytes(StandardCharsets.UTF_8);
    return ByteBuffer.allocate(4 + body.length).putInt(body.length).put(body).array();
}

/** Collects bytes as TCP delivers them and returns each message once it is complete. */
class FrameDecoder {
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();

    List<String> feed(byte[] chunk) {
        pending.writeBytes(chunk); // a read may hold half a message, or three of them
        ByteBuffer buf = ByteBuffer.wrap(pending.toByteArray());
        List<String> messages = new ArrayList<>();
        while (buf.remaining() >= 4) {
            int length = buf.getInt(buf.position()); // peek; don't consume yet
            if (buf.remaining() < 4 + length) break; // body not fully here
            buf.position(buf.position() + 4);
            byte[] body = new byte[length];
            buf.get(body);
            messages.add(new String(body, StandardCharsets.UTF_8));
        }
        pending.reset();
        pending.write(buf.array(), buf.position(), buf.remaining()); // keep the leftover
        return messages;
    }
}
