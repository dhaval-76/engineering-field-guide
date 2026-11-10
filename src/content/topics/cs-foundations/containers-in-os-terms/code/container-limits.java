/** The CPU and memory limits of the current container, read from cgroup v2 files. */
record Limits(Double cpus, Long memoryBytes, long throttledPeriods) {}

Limits readLimits(Path cgroup) throws IOException { // normally /sys/fs/cgroup
    // cpu.max is "quota period" in microseconds, or "max period" for no limit
    String[] cpu = Files.readString(cgroup.resolve("cpu.max")).trim().split(" ");
    Double cpus = cpu[0].equals("max") ? null : Double.parseDouble(cpu[0]) / Double.parseDouble(cpu[1]);

    String mem = Files.readString(cgroup.resolve("memory.max")).trim();
    Long memoryBytes = mem.equals("max") ? null : Long.parseLong(mem);

    long throttled = 0; // periods in which the quota ran out and the cgroup was paused
    for (String line : Files.readAllLines(cgroup.resolve("cpu.stat"))) {
        if (line.startsWith("nr_throttled ")) throttled = Long.parseLong(line.split(" ")[1]);
    }
    return new Limits(cpus, memoryBytes, throttled);
}
