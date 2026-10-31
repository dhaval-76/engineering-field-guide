@Component
class JobRunner {
    private static final Duration LEASE = Duration.ofSeconds(60);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Map<String, JobHandler> handlers;
    private final String workerId = "worker-" + UUID.randomUUID();

    /** Claim up to `batch` due jobs in my partitions; commit the claim before running anything. */
    List<Claimed> claim(List<Integer> partitions, int batch) {
        return tx.execute(s -> jdbc.query("""
            UPDATE jobs j SET status = 'RUNNING', lease_owner = ?, lease_until = now() + ?::interval,
                              attempts = attempts + 1, fence = fence + 1
            WHERE j.id IN (
                SELECT id FROM jobs
                WHERE partition = ANY(?::int[]) AND run_at <= now()
                  AND (status = 'PENDING' OR (status = 'RUNNING' AND lease_until < now()))   -- expired lease
                ORDER BY priority DESC, run_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED)
            RETURNING id, type, payload, attempts, max_attempts, fence""",
            (rs, i) -> new Claimed(rs.getObject("id", UUID.class), rs.getString("type"), rs.getString("payload"),
                                   rs.getInt("attempts"), rs.getInt("max_attempts"), rs.getLong("fence")),
            workerId, LEASE.toSeconds() + " seconds",
            // '{0,1,2}'
            partitions.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}")),
            batch));
    }

    void run(Claimed job) {
        ScheduledFuture<?> heartbeat = heartbeats.scheduleAtFixedRate(() -> jdbc.update(
            "UPDATE jobs SET lease_until = now() + ?::interval WHERE id = ? AND fence = ?",
            LEASE.toSeconds() + " seconds", job.id(), job.fence()), 20, 20, TimeUnit.SECONDS);
        try {
            // idempotent; fence passed to resources
            handlers.get(job.type()).handle(job.payload(), job.fence());
            finish(job, "SUCCEEDED", null, null);
        } catch (RetryableException e) {
            boolean exhausted = job.attempts() >= job.maxAttempts();
            Duration backoff = Duration.ofSeconds((long) Math.min(3600, Math.pow(2, job.attempts()) * (0.5 + Math.random())));
            finish(job, exhausted ? "DEAD" : "PENDING", e.getMessage(), exhausted ? null : backoff);
        } catch (Exception permanent) {
            finish(job, "DEAD", permanent.toString(), null);
        } finally {
            heartbeat.cancel(false);
        }
    }

    /** Only the current lease holder (matching fence) may record the outcome. */
    private void finish(Claimed job, String status, String error, Duration retryIn) {
        int updated = jdbc.update("""
            UPDATE jobs SET status = ?, last_error = ?, lease_owner = NULL, lease_until = NULL,
                            run_at = COALESCE(now() + ?::interval, run_at)
            WHERE id = ? AND fence = ?""",
            status, error, retryIn == null ? null : retryIn.toSeconds() + " seconds", job.id(), job.fence());
        if (updated == 0) log.warn("lost lease on {} (fence {}); result discarded", job.id(), job.fence());
        if ("DEAD".equals(status)) alerts.deadJob(job.id(), error);
    }
}
