/** Request headers a page may send cross-origin without a preflight (Fetch standard). */
static final Set<String> SAFELISTED =
        Set.of("accept", "accept-language", "content-language", "content-type", "range");
static final Set<String> SIMPLE_TYPES = Set.of(
        "application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

/** Does a browser send an OPTIONS preflight before this cross-origin fetch? */
boolean needsPreflight(String method, Map<String, String> headers) {
    if (!Set.of("GET", "HEAD", "POST").contains(method)) return true;
    for (var h : headers.entrySet()) {
        String name = h.getKey().toLowerCase(), value = h.getValue();
        if (!SAFELISTED.contains(name)) return true; // e.g. Authorization, X-Request-Id
        if (name.equals("content-type")) {
            String type = value.split(";")[0].trim().toLowerCase();
            if (!SIMPLE_TYPES.contains(type)) return true; // e.g. application/json
        }
        if (name.equals("range") && !value.matches("bytes=\\d+-\\d*")) return true;
    }
    return false;
}
