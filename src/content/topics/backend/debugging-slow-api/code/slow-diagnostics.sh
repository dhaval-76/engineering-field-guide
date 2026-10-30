# --- JVM: what are the threads doing right now? (take 3 dumps ~5 s apart and compare)
jcmd <pid> Thread.print > dump-1.txt            # or: jstack <pid>, or GET /actuator/threaddump
#   many threads in HikariPool.getConnection  → waiting for DB connections
#   many in SocketInputStream.read / epollWait under a client class → slow downstream
#   many BLOCKED on the same lock             → contention

# --- JVM: where is CPU (or wall-clock) time going? 30-second flame graph
asprof -d 30 -e cpu  -f cpu.html  <pid>         # async-profiler
asprof -d 30 -e wall -f wall.html <pid>         # includes time spent waiting
# or record with Java Flight Recorder and open in JDK Mission Control:
jcmd <pid> JFR.start duration=60s filename=slow.jfr

# --- JVM: is GC pausing the app?
#   start the JVM with -Xlog:gc*:file=gc.log:time,uptime  and look for long pauses / back-to-back collections
jcmd <pid> GC.heap_info

# --- PostgreSQL: which queries cost the most in total?
psql -c "SELECT calls, round(mean_exec_time) AS mean_ms, round(total_exec_time) AS total_ms, query
         FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;"
# …and why is this one slow?
psql -c "EXPLAIN (ANALYZE, BUFFERS) SELECT … ;"   # look for Seq Scan on big tables, row estimate vs actual
# who is waiting on locks or idle in a transaction?
psql -c "SELECT pid, state, wait_event_type, wait_event, now() - xact_start AS in_tx, query
         FROM pg_stat_activity WHERE state <> 'idle' ORDER BY in_tx DESC;"
