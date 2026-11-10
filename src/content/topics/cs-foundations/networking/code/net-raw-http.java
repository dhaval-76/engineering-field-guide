/** Sends GET over a plain TCP socket and returns the status line, e.g. "HTTP/1.1 200 OK". */
String statusLine(String host, int port, String path) throws IOException {
    try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(host, port), 2000); // TCP handshake
        socket.setSoTimeout(2000);                               // don't wait forever to read
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + "\r\n"                       // required in HTTP/1.1
                + "Connection: close\r\n"
                + "\r\n";                                        // blank line ends the headers
        socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        return in.readLine(); // the bytes are just text with a fixed shape
    }
}
