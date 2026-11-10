/** Opens a TLS connection and returns how many days the server's certificate has left. */
long daysLeft(String host) throws IOException {
    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    try (SSLSocket socket = (SSLSocket) factory.createSocket(host, 443)) {
        SSLParameters params = socket.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS"); // check the name, not just the chain
        socket.setSSLParameters(params);
        socket.startHandshake(); // throws if the chain or name doesn't verify
        X509Certificate leaf = (X509Certificate) socket.getSession().getPeerCertificates()[0];
        return Duration.between(Instant.now(), leaf.getNotAfter().toInstant()).toDays();
    }
}
