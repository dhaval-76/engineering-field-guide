// Plain java.net.http client against the Anthropic Messages API; other providers look
// similar.
record Ticket(String category, String priority, String summary) {}

private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();
private static final ObjectMapper JSON = new ObjectMapper();
private static final Set<String> CATEGORIES = Set.of("billing", "bug", "account", "other");

Ticket classify(String ticketText) throws Exception {
    String system = """
        You classify support tickets. Reply with JSON only:
        {"category": "billing|bug|account|other", "priority": "low|medium|high", "summary": "<= 20 words"}
        """;
    Map<String, Object> body = Map.of(
            "model", "claude-sonnet-5-5",
            "max_tokens", 200,                       // always cap output
            "temperature", 0,                        // classification: be consistent
            "system", system,
            "messages", List.of(Map.of("role", "user",
                    // delimit untrusted text
                    "content", "<ticket>\n" + ticketText + "\n</ticket>")));

    String text = callWithRetry(JSON.writeValueAsString(body));
    // throws if not valid JSON / wrong shape
    Ticket t = JSON.readValue(text, Ticket.class);
    if (!CATEGORIES.contains(t.category())) throw new IllegalStateException("bad category " + t.category());
    return t;
}

String callWithRetry(String json) throws Exception {
    HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(30))
            .header("x-api-key", System.getenv("ANTHROPIC_API_KEY"))
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();

    for (int attempt = 0; ; attempt++) {
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        int s = res.statusCode();
        if (s == 200) {
            JsonNode root = JSON.readTree(res.body());
            log.info("llm usage in={} out={}", root.at("/usage/input_tokens"), root.at("/usage/output_tokens"));
            return root.at("/content/0/text").asText();
        }
        boolean retryable = s == 429 || s >= 500;
        if (!retryable || attempt == 3) throw new IOException("LLM call failed: " + s + " " + res.body());
        long waitMs = res.headers().firstValue("retry-after").map(v -> Long.parseLong(v) * 1000)
                .orElse((long) (Math.pow(2, attempt) * 500 + ThreadLocalRandom.current().nextInt(250)));
        Thread.sleep(waitMs);
    }
}
