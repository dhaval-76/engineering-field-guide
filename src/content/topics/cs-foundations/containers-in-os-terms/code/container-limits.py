from pathlib import Path


def read_limits(cgroup: str = "/sys/fs/cgroup") -> dict:
    """The CPU and memory limits of the current container, read from cgroup v2 files."""
    root = Path(cgroup)
    # cpu.max is "quota period" in microseconds, or "max period" for no limit
    quota, period = (root / "cpu.max").read_text().split()
    cpus = None if quota == "max" else int(quota) / int(period)

    mem = (root / "memory.max").read_text().strip()
    memory_bytes = None if mem == "max" else int(mem)

    throttled = 0  # periods in which the quota ran out and the cgroup was paused
    for line in (root / "cpu.stat").read_text().splitlines():
        if line.startswith("nr_throttled "):
            throttled = int(line.split()[1])
    return {"cpus": cpus, "memory_bytes": memory_bytes, "throttled_periods": throttled}
