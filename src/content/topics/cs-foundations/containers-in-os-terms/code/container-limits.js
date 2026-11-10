import fs from 'node:fs';
import path from 'node:path';

/** The CPU and memory limits of the current container, read from cgroup v2 files. */
function readLimits(cgroup = '/sys/fs/cgroup') {
  const read = (file) => fs.readFileSync(path.join(cgroup, file), 'utf8').trim();
  // cpu.max is "quota period" in microseconds, or "max period" for no limit
  const [quota, period] = read('cpu.max').split(' ');
  const cpus = quota === 'max' ? null : Number(quota) / Number(period);

  const mem = read('memory.max');
  const memoryBytes = mem === 'max' ? null : Number(mem);

  // periods in which the quota ran out and the cgroup was paused
  const line = read('cpu.stat').split('\n').find((l) => l.startsWith('nr_throttled '));
  const throttledPeriods = line ? Number(line.split(' ')[1]) : 0;
  return { cpus, memoryBytes, throttledPeriods };
}
