// Relationship tuples (Zanzibar-style), e.g. in OpenFGA / SpiceDB:
//   folder:roadmap#editor@user:asha
//   document:q3-plan#parent@folder:roadmap
// Model: a document's editors include editors of its parent folder.

@Service
class DocumentAccess {
    // OpenFGA/SpiceDB client, replicated service
    private final PermissionClient permissions;
    // short TTL: revocations take effect within seconds
    private final Cache<String, Boolean> recent =
        Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(5)).maximumSize(100_000).build();

    public void requireEdit(String userId, String docId) {
        String cacheKey = userId + "|edit|" + docId;
        boolean allowed = recent.get(cacheKey, k -> permissions.check("user:" + userId, "editor", "document:" + docId));
        if (!allowed) throw new AccessDeniedException("document " + docId);
    }

    /** List pages: one batched call, not one check per row. */
    public List<Document> visible(String userId, List<Document> candidates) {
        Set<String> allowed = permissions.batchCheck("user:" + userId, "viewer",
                candidates.stream().map(d -> "document:" + d.id()).toList());
        return candidates.stream().filter(d -> allowed.contains("document:" + d.id())).toList();
    }

    public void share(String docId, String withUserId) {
        // a new tuple
        permissions.write("document:" + docId, "editor", "user:" + withUserId);
    }
}
