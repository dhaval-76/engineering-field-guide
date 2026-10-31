# Heap sizing (containers: prefer percentages of the memory limit)
java -XX:MaxRAMPercentage=75 -jar app.jar
java -Xms4g -Xmx4g -jar app.jar                 # fixed-size heap: no resize pauses

# Choosing a collector
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 ...   # default: balanced, pause-time goal
# sub-millisecond pauses (generational by default since JDK 23)
java -XX:+UseZGC ...
java -XX:+UseParallelGC ...                      # maximum throughput for batch jobs

# Evidence before tuning
java -Xlog:gc*:file=gc.log:time,uptime,level,tags ...           # GC log
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps ...  # dump on OOM

# On a running JVM
jcmd <pid> GC.heap_info
# which classes use the most memory right now
jcmd <pid> GC.class_histogram | head -30
jcmd <pid> GC.heap_dump /tmp/heap.hprof       # analyse with Eclipse MAT / VisualVM
# Java Flight Recorder: allocation + GC profiling
jcmd <pid> JFR.start duration=60s filename=rec.jfr
