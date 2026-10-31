// Kafka Streams: revenue per product per minute, by event time, exactly-once, late data
// up to 5 minutes. props: processing.guarantee=exactly_once_v2,
// default.timestamp.extractor=PurchaseTimeExtractor (event time)
StreamsBuilder b = new StreamsBuilder();

b.stream("purchases", Consumed.with(Serdes.String(), purchaseSerde))
 .groupBy((k, p) -> p.productId(), Grouped.with(Serdes.String(), purchaseSerde))
 .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(1), Duration.ofMinutes(5)))
 .aggregate(Revenue::zero,
            (productId, p, acc) -> acc.add(p.amountMinor()),
            Materialized.with(Serdes.String(), revenueSerde))
 // emit once per window
 .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
 .toStream()
 // idempotent upsert key
 .map((w, rev) -> KeyValue.pair(w.key() + "@" + w.window().startTime(), rev))
 .to("revenue-per-minute");

// Flink equivalent (DataStream API):
// purchases
//   .assignTimestampsAndWatermarks(WatermarkStrategy
//       .<Purchase>forBoundedOutOfOrderness(Duration.ofSeconds(10))
//       .withTimestampAssigner((p, ts) -> p.eventTimeMillis()))
//   .keyBy(Purchase::productId)
//   .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
//   .allowedLateness(Duration.ofMinutes(5))
//   .sideOutputLateData(tooLate)
//   .aggregate(new RevenueAggregate())
//   .sinkTo(clickhouseUpsertSink);
