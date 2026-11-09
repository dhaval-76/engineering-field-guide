# CS Foundations probes

Throwaway programs behind the numbers in `../../cs-foundations-2026-11.md`. Not part of the site.

Compile for Java 21, then run inside a container so the results come from Linux:

```bash
javac --release 21 -d out M.java P.java
docker run --rm --cpus=1 -v "$PWD/out":/m -w /m --entrypoint java apache/kafka:4.1.0 M stall 4 3000
```

`M` modes: `nagle`, `stall <threads> <ms>`, `info`, `oom`, `fsync <n>`, `sharing`, `access`,
`pingpong`, `spawn`, `zc`. Add `--memory` and `--cpus` to the `docker run` line to test limits.

`P pc` is the page cache test and needs `--user root --privileged`.

Known bad: `M timewait`, `P tw` (always report 0) and `M pagecache` (needs root and measures a warm
file). Don't use their output.
