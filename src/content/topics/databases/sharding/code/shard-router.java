/** Maps a tenant to a physical database through 1,024 fixed virtual shards. */
final class ShardRouter {
    // fixed forever; physical layout can change
    static final int VIRTUAL_SHARDS = 1024;

    // e.g. 0–255 → db-a, 256–511 → db-b …
    private final Map<Integer, String> bucketToDatabase;
    // directory overrides: whales get their own DB
    private final Map<Long, String> pinnedTenants;

    ShardRouter(Map<Integer, String> bucketToDatabase, Map<Long, String> pinnedTenants) {
        this.bucketToDatabase = Map.copyOf(bucketToDatabase);
        this.pinnedTenants = Map.copyOf(pinnedTenants);
    }

    static int bucketOf(long tenantId) {
        // A stable hash — NOT Object.hashCode, which may differ between languages and
        // versions.
        long h = Hashing.murmur3_128().hashLong(tenantId).asLong();   // Guava
        return (int) Long.remainderUnsigned(h, VIRTUAL_SHARDS);
    }

    String databaseFor(long tenantId) {
        String pinned = pinnedTenants.get(tenantId);
        return pinned != null ? pinned : bucketToDatabase.get(bucketOf(tenantId));
    }
}

// Adding a database: copy buckets 192–255 from db-a to db-e (snapshot + change stream),
// verify, then update bucketToDatabase for those 64 buckets. No key is rehashed.

// Spring: route each request's connection by tenant.
class TenantRoutingDataSource extends AbstractRoutingDataSource {
    private final ShardRouter router;
    TenantRoutingDataSource(ShardRouter router) { this.router = router; }

    @Override
    protected Object determineCurrentLookupKey() {
        return router.databaseFor(TenantContext.required());   // set by a request filter
    }
}
