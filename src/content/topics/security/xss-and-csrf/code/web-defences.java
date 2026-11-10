/** Encodes text for an HTML body or a quoted attribute, so it can never become markup. */
String escapeHtml(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
        switch (c) {
            case '&' -> out.append("&amp;");
            case '<' -> out.append("&lt;");
            case '>' -> out.append("&gt;");
            case '"' -> out.append("&quot;");
            case '\'' -> out.append("&#x27;");
            default -> out.append(c);
        }
    }
    return out.toString();
}

/**
 * CSRF check for a state-changing request. The browser says where the request came from
 * (Sec-Fetch-Site, or Origin in older browsers), and the form must echo the session's token.
 * Header names are lower-case here.
 */
boolean allowStateChange(String method, Map<String, String> headers, String ourOrigin,
        String sessionToken, String sentToken) {
    if (Set.of("GET", "HEAD", "OPTIONS").contains(method)) return true; // must change nothing
    String site = headers.get("sec-fetch-site");
    if (site != null) {
        if (!site.equals("same-origin") && !site.equals("none")) return false; // another site
    } else if (!ourOrigin.equals(headers.get("origin"))) {
        return false;
    }
    return sentToken != null && MessageDigest.isEqual( // constant-time comparison
            sessionToken.getBytes(StandardCharsets.UTF_8),
            sentToken.getBytes(StandardCharsets.UTF_8));
}
