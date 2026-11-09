/** Only operations registered at build time can run. Clients send a hash, not a query. */
class TrustedDocuments {
    private final Map<String, String> byHash = new HashMap<>();

    /** Called from the client's build, for each operation it contains. */
    String register(String document) throws NoSuchAlgorithmException {
        String hash = sha256(document);
        byHash.put(hash, document);
        return hash;
    }

    /** Empty means reject: this query did not come from a build of our own app. */
    Optional<String> lookup(String hash) {
        return Optional.ofNullable(byHash.get(hash));
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        byte[] digest =
                MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
