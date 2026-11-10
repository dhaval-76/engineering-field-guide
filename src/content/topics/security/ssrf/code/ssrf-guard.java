/**
 * Resolves a user-supplied URL once and returns the address to connect to, or throws if it
 * points inside our network. Connect to this address (sending the Host header), not to the
 * name again: a second lookup could return something else (DNS rebinding).
 */
InetAddress safeDestination(String url) throws IOException {
    URI uri = URI.create(url);
    if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
        throw new IOException("scheme not allowed: " + uri.getScheme()); // file:, gopher:, …
    }
    if (uri.getHost() == null) throw new IOException("no host");
    InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
    for (InetAddress a : addresses) { // every answer must be safe, not just the first
        if (isInternal(a)) throw new IOException("internal address: " + a.getHostAddress());
    }
    return addresses[0];
}

boolean isInternal(InetAddress a) {
    byte[] b = a.getAddress();
    if (b.length == 4) {
        int x = b[0] & 0xFF, y = b[1] & 0xFF;
        return x == 0 || x == 10 || x == 127                // this host, private, loopback
                || (x == 169 && y == 254)                   // link-local: cloud metadata
                || (x == 172 && y >= 16 && y <= 31)         // private
                || (x == 192 && y == 168)                   // private
                || (x == 100 && y >= 64 && y <= 127)        // carrier-grade NAT
                || x >= 224;                                // multicast and reserved
    }
    return a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress()
            || a.isMulticastAddress() || (b[0] & 0xFE) == 0xFC; // fc00::/7 unique local
}
