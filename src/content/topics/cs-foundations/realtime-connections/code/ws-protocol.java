static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; // fixed by RFC 6455

/** The server's Sec-WebSocket-Accept: proof it understood this upgrade, not a cached reply. */
String acceptKey(String secWebSocketKey) throws NoSuchAlgorithmException {
    byte[] sha1 = MessageDigest.getInstance("SHA-1")
            .digest((secWebSocketKey + GUID).getBytes(StandardCharsets.US_ASCII));
    return Base64.getEncoder().encodeToString(sha1);
}

/** One complete text frame. Clients must mask with 4 random bytes; servers must not. */
byte[] textFrame(String text, byte[] mask) {
    byte[] payload = text.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(0x81); // FIN = last fragment, opcode 1 = text
    int maskBit = mask == null ? 0 : 0x80;
    if (payload.length < 126) {
        out.write(maskBit | payload.length);
    } else if (payload.length < 65536) {
        out.write(maskBit | 126); // length follows in 2 bytes
        out.write(payload.length >> 8);
        out.write(payload.length & 0xFF);
    } else {
        out.write(maskBit | 127); // length follows in 8 bytes
        out.writeBytes(ByteBuffer.allocate(8).putLong(payload.length).array());
    }
    if (mask != null) {
        out.writeBytes(mask);
        for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
    }
    out.writeBytes(payload);
    return out.toByteArray();
}
