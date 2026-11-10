record Reply(int status, Map<String, String> headers, String body) {}

/** Serves a cacheable resource, answering 304 Not Modified when the client's copy is current. */
Reply serve(String body, String ifNoneMatch) throws NoSuchAlgorithmException {
    byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(body.getBytes(StandardCharsets.UTF_8));
    String etag = "\"" + HexFormat.of().formatHex(digest, 0, 8) + "\""; // a version id
    Map<String, String> headers = Map.of(
            "ETag", etag,
            // reuse for 60 s without asking; afterwards revalidate with If-None-Match
            "Cache-Control", "max-age=60");
    if (ifNoneMatch != null && matches(ifNoneMatch, etag)) {
        return new Reply(304, headers, ""); // no body: the client already has it
    }
    return new Reply(200, headers, body);
}

/** If-None-Match may list several tags, or "*"; W/ (weak) tags compare by value. */
boolean matches(String ifNoneMatch, String etag) {
    for (String tag : ifNoneMatch.split(",")) {
        String t = tag.trim();
        if (t.equals("*") || t.replaceFirst("^W/", "").equals(etag)) return true;
    }
    return false;
}
