// Route @Transactional(readOnly = true) to a replica, everything else to the primary.
class ReplicaRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        return readOnly && !RecentWrites.mustUsePrimary() ? "replica" : "primary";
    }
}

@Configuration
class DataSourceConfig {
    @Bean
    DataSource dataSource(DataSource primary, DataSource replica) {
        var routing = new ReplicaRoutingDataSource();
        routing.setTargetDataSources(Map.of("primary", primary, "replica", replica));
        routing.setDefaultTargetDataSource(primary);
        routing.afterPropertiesSet();
        // Essential: JPA grabs a connection when the transaction BEGINS, before the
        // read-only flag is visible. The lazy proxy delays it until the first statement.
        return new LazyConnectionDataSourceProxy(routing);
    }
}

// Read-your-writes: after this user writes, send their reads to the primary briefly.
// (Request-scoped here; across instances, keep the marker in a cookie or Redis.)
final class RecentWrites {
    private static final Duration WINDOW = Duration.ofSeconds(5);
    private static final ThreadLocal<Instant> lastWrite = new ThreadLocal<>();
    static void markWrite(Instant at) { lastWrite.set(at); }
    static boolean mustUsePrimary() {
        Instant t = lastWrite.get();
        return t != null && t.plus(WINDOW).isAfter(Instant.now());
    }
}
// A filter reads the "last_write_at" cookie into RecentWrites; the write path sets it.
