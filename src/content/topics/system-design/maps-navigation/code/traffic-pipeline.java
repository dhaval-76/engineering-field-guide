// Flink: GPS probes → matched segments → speed per segment per minute → routing weights
DataStream<Probe> probes = env.fromSource(kafkaSource("gps-probes"), watermarks(Duration.ofSeconds(30)), "probes");

probes
    .keyBy(Probe::sessionId)                               // consecutive points of one anonymous session
    .process(new MapMatcher(roadIndex))                    // snap noisy points to the travelled segments (HMM)
    .keyBy(SegmentTraversal::segmentId)
    .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
    .aggregate(new MedianSpeed())                          // robust to outliers (a parked car, a GPS jump)
    .filter(s -> s.sampleCount() >= 5)                     // privacy and noise: require several vehicles
    .map(s -> new WeightUpdate(s.segmentId(),
            blend(s.medianSpeed(), historical.speedFor(s.segmentId(), s.windowStart()))))
    .sinkTo(kafkaSink("segment-weights"));                 // routing servers apply updates every ~minute

// blend: trust live speeds when there are many samples, fall back towards history when sparse.
