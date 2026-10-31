// Flink: GPS probes → matched segments → speed per segment per minute → routing weights
DataStream<Probe> probes =
        env.fromSource(
                kafkaSource("gps-probes"), watermarks(Duration.ofSeconds(30)), "probes");

probes
        // consecutive points of one anonymous session
        .keyBy(Probe::sessionId)
        // snap noisy points to the travelled segments (HMM)
        .process(new MapMatcher(roadIndex))
        .keyBy(SegmentTraversal::segmentId)
        .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
        // robust to outliers (a parked car, a GPS jump)
        .aggregate(new MedianSpeed())
        // privacy and noise: require several vehicles
        .filter(s -> s.sampleCount() >= 5)
        .map(
                s ->
                        new WeightUpdate(
                                s.segmentId(),
                                blend(
                                        s.medianSpeed(),
                                        historical.speedFor(
                                                s.segmentId(), s.windowStart()))))
        // routing servers apply updates every ~minute
        .sinkTo(kafkaSink("segment-weights"));

// blend: trust live speeds when there are many samples, fall back towards history when
// sparse.

