/** One breaking change: how to turn a response from after this date into one from before it. */
record VersionChange(
        String date, String description, UnaryOperator<Map<String, Object>> downgrade) {}

/**
 * The code only ever builds the newest response. Older clients get it walked back through each
 * change newer than their pinned version, so old versions cost one small function each.
 */
Map<String, Object> renderFor(
        Map<String, Object> latest, String clientVersion, List<VersionChange> newestFirst) {
    Map<String, Object> response = new LinkedHashMap<>(latest);
    for (VersionChange change : newestFirst) {
        if (change.date().compareTo(clientVersion) <= 0) break;
        response = change.downgrade().apply(response);
    }
    return response;
}
