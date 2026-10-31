PID=$(pgrep -f orders-service | head -1)

# Threads: how many, and what state
ls /proc/$PID/task | wc -l
top -H -p $PID                          # per-thread CPU

# Memory: resident vs virtual, page faults
grep -E 'VmRSS|VmSize|Threads' /proc/$PID/status
ps -o pid,rss,vsz,maj_flt,min_flt -p $PID
free -h                                 # "buff/cache" is page cache — reclaimable

# File descriptors and their limit
ls /proc/$PID/fd | wc -l
grep 'open files' /proc/$PID/limits

# Context switches (voluntary = waiting on I/O/locks, involuntary = preempted)
grep ctxt /proc/$PID/status

# Container limits (cgroup v2), from inside the container
cat /sys/fs/cgroup/memory.max /sys/fs/cgroup/cpu.max
# nr_throttled / throttled_usec: CPU-limit throttling
cat /sys/fs/cgroup/cpu.stat
