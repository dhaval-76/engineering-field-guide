// Step builder: each step returns an interface exposing only the next legal call,
// so the compiler enforces "url, then method, then (body only for POST/PUT), then build".
public final class HttpRequest {
    public interface UrlStep    { MethodStep url(String url); }
    public interface MethodStep { BuildStep get(); BodyStep post(); BodyStep put(); }
    public interface BodyStep   { BuildStep body(String json); }
    public interface BuildStep {
        BuildStep header(String k, String v);
        BuildStep timeout(Duration d);
        HttpRequest build();
    }

    public static UrlStep newRequest() { return new Steps(); }

    private static final class Steps implements UrlStep, MethodStep, BodyStep, BuildStep {
        private String url, method, body;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Duration timeout = Duration.ofSeconds(5);
        public MethodStep url(String u)          { url = u; return this; }
        public BuildStep get()                   { method = "GET"; return this; }
        public BodyStep post()                   { method = "POST"; return this; }
        public BodyStep put()                    { method = "PUT"; return this; }
        public BuildStep body(String json)       { body = json; return this; }
        public BuildStep header(String k, String v) { headers.put(k, v); return this; }
        public BuildStep timeout(Duration d)     { timeout = d; return this; }
        public HttpRequest build() {
            return new HttpRequest(url, method, body, headers, timeout);
        }
    }
    private HttpRequest(String url, String method, String body,
                        Map<String, String> h, Duration t) { /* assign, copy map */ }
}

HttpRequest r = HttpRequest.newRequest().url("https://api.example.com/orders").post().body("{}").build();
// HttpRequest.newRequest().url("…").get().body("{}")   ← does not compile: GET has no
// body step
