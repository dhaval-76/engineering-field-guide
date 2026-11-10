record Answer(String address, int ttl) {}

/** A DNS question for a name's IPv4 addresses, built by hand. */
byte[] buildQuery(String name) {
    ByteBuffer q = ByteBuffer.allocate(512);
    q.putShort((short) 0x1234)   // id, echoed back in the reply
     .putShort((short) 0x0100)   // flags: recursion desired
     .putShort((short) 1)        // one question
     .putShort((short) 0).putShort((short) 0).putShort((short) 0);
    for (String label : name.split("\\.")) { // "example.com" -> 7example3com0
        q.put((byte) label.length()).put(label.getBytes(StandardCharsets.US_ASCII));
    }
    q.put((byte) 0).putShort((short) 1).putShort((short) 1); // type A, class IN
    return Arrays.copyOf(q.array(), q.position());
}

/** Reads the A records (address and remaining TTL) out of a reply. */
List<Answer> parseReply(byte[] reply) throws UnknownHostException {
    ByteBuffer r = ByteBuffer.wrap(reply);
    int answers = r.getShort(6) & 0xFFFF;
    r.position(skipName(r, 12) + 4); // past the echoed question
    List<Answer> out = new ArrayList<>();
    for (int i = 0; i < answers; i++) {
        r.position(skipName(r, r.position()));
        int type = r.getShort() & 0xFFFF;
        r.getShort(); // class
        int ttl = r.getInt(), length = r.getShort() & 0xFFFF;
        byte[] data = new byte[length];
        r.get(data);
        if (type == 1) out.add(new Answer(InetAddress.getByAddress(data).getHostAddress(), ttl));
        // type 5 is a CNAME: the A records for its target follow
    }
    return out;
}

/** Names are labels, or a 2-byte pointer (top bits 11) to a name earlier in the packet. */
int skipName(ByteBuffer r, int at) {
    while (true) {
        int len = r.get(at) & 0xFF;
        if (len == 0) return at + 1;
        if ((len & 0xC0) == 0xC0) return at + 2;
        at += len + 1;
    }
}

/** One UDP packet out, one back. No reply in time means ask again or give up. */
List<Answer> lookup(String name, String resolver) throws IOException {
    try (DatagramSocket socket = new DatagramSocket()) {
        socket.setSoTimeout(2000);
        byte[] query = buildQuery(name);
        socket.send(new DatagramPacket(query, query.length, new InetSocketAddress(resolver, 53)));
        DatagramPacket reply = new DatagramPacket(new byte[512], 512);
        socket.receive(reply);
        return parseReply(Arrays.copyOf(reply.getData(), reply.getLength()));
    }
}
