@Service
class PageVersions {
    private static final int SNAPSHOT_EVERY = 10;
    private final BlobStore blobs;
    private final VersionRepository versions;
    private final JdbcTemplate jdbc;

    @Transactional
    public long save(
            long pageId, long expectedHead, String content, long authorId, String message) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] hash = sha256(bytes);
        // identical content (or a restore) stores nothing
        if (!blobs.exists(hash)) {
            Blob parent = blobs.get(versions.get(expectedHead).contentHash());
            if (parent.chainLength() + 1 >= SNAPSHOT_EVERY) blobs.putFull(hash, bytes);
            else
                blobs.putDelta(
                        hash,
                        parent.hash(),
                        Delta.between(blobs.read(parent.hash()), bytes),
                        parent.chainLength() + 1);
        }
        long id = versions.insert(pageId, expectedHead, hash, authorId, message);
        int moved =
                jdbc.update(
                        "UPDATE pages SET head_id = ? WHERE id = ? AND head_id = ?",
                        id,
                        pageId,
                        expectedHead);
        // rolls back; client must rebase or merge
        if (moved == 0) throw new ConcurrentEditException(pageId);
        return id;
    }

    /** Rebuild content: walk back to the nearest full snapshot, then apply deltas forwards. */
    public String read(byte[] hash) {
        Deque<Blob> chain = new ArrayDeque<>();
        Blob b = blobs.get(hash);
        while (b.isDelta()) {
            chain.push(b);
            b = blobs.get(b.baseHash());
        }
        byte[] content = b.data();
        while (!chain.isEmpty()) content = Delta.apply(content, chain.pop().data());
        return new String(content, StandardCharsets.UTF_8);
    }
}
