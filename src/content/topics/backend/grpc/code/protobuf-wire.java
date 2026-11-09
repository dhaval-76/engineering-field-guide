/** Protobuf's variable-length integer: 7 bits per byte, low bits first, top bit means "more". */
void writeVarint(ByteArrayOutputStream out, long value) {
    while ((value & ~0x7FL) != 0) {
        out.write((int) (value & 0x7F) | 0x80);
        value >>>= 7;
    }
    out.write((int) value);
}

/** Every field starts with its number and wire type. Names are never sent. */
void writeTag(ByteArrayOutputStream out, int fieldNumber, int wireType) {
    writeVarint(out, ((long) fieldNumber << 3) | wireType);
}

void writeInt(ByteArrayOutputStream out, int fieldNumber, long value) {
    writeTag(out, fieldNumber, 0); // wire type 0: varint
    writeVarint(out, value);
}

void writeString(ByteArrayOutputStream out, int fieldNumber, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeTag(out, fieldNumber, 2); // wire type 2: length-delimited
    writeVarint(out, bytes.length);
    out.writeBytes(bytes);
}

/** Returns {value, offset of the next byte}. */
long[] readVarint(byte[] data, int at) {
    long value = 0;
    for (int shift = 0; ; shift += 7) {
        byte b = data[at++];
        value |= (b & 0x7FL) << shift;
        if ((b & 0x80) == 0) return new long[] {value, at};
    }
}

/** gRPC's frame: 1 byte "compressed?" flag, 4 bytes big-endian length, then the message. */
byte[] grpcFrame(byte[] message) {
    return ByteBuffer.allocate(5 + message.length)
            .put((byte) 0)
            .putInt(message.length)
            .put(message)
            .array();
}
