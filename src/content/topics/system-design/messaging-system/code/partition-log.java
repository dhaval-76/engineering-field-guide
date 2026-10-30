/** One partition: segments named by base offset, each with a sparse index. Single writer per partition. */
final class PartitionLog {
    private static final long SEGMENT_BYTES = 1L << 30;     // roll to a new file at 1 GB
    private static final int INDEX_EVERY_BYTES = 4096;       // one index entry per ~4 KB of log

    private final Path dir;
    private final NavigableMap<Long, Segment> segments = new ConcurrentSkipListMap<>();
    private Segment active;
    private long nextOffset;

    long append(byte[] key, byte[] value) throws IOException {
        if (active.sizeBytes() >= SEGMENT_BYTES) roll();
        long offset = nextOffset++;
        int position = active.appendRecord(offset, key, value);   // sequential write to the file
        if (position - active.lastIndexedPosition() >= INDEX_EVERY_BYTES)
            active.index(offset, position);                         // sparse: offset → byte position
        return offset;
    }

    List<Record> read(long fromOffset, int maxBytes) throws IOException {
        Segment seg = segments.floorEntry(fromOffset).getValue();  // which file holds it
        int start = seg.positionNear(fromOffset);                   // binary search the sparse index
        return seg.scanFrom(start, fromOffset, maxBytes);           // short sequential scan
    }

    void enforceRetention(Instant cutoff) throws IOException {
        // Delete whole old segments — never individual messages.
        for (var it = segments.headMap(active.baseOffset()).values().iterator(); it.hasNext(); ) {
            Segment s = it.next();
            if (s.lastModified().isBefore(cutoff)) { s.delete(); it.remove(); }
        }
    }

    private void roll() throws IOException {
        active.flushAndClose();
        active = Segment.create(dir, nextOffset);                   // e.g. 00000000000005368709.log + .index
        segments.put(nextOffset, active);
    }
}
