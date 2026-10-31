// Java 11+ HttpClient: one shared instance reuses connections (keep-alive) and negotiates
// HTTP/2.
static final HttpClient CLIENT = HttpClient.newBuilder()
    // falls back to HTTP/1.1 if the server can't
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofMillis(500))         // TCP + TLS must complete within this
    .build();

String fetchCart(String sessionId) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder(URI.create("https://shop.example.com/cart"))
        .timeout(Duration.ofSeconds(2))             // whole response
        .header("Cookie", "session=" + sessionId)
        .build();
    return CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body();
}
// ✗ Creating a new client (or a new connection) per call pays DNS + TCP + TLS every time.
// JVM DNS caching: networkaddress.cache.ttl (security property) — e.g. 30 s for services
// behind DNS failover.
