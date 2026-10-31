// ✗ One object for every destination.
// logger.log(m); logger.logToFile(m); logger.logToDb(m); logger.logToSplunk(m);

// ✓ One small contract — a sink is anything with write(event); destinations are separate sinks.
const consoleSink = { write: (e) => console.log(JSON.stringify(e)) };
const fileSink = (stream) => ({ write: (e) => stream.write(JSON.stringify(e) + '\n') });
const splunkSink = (http) => ({ write: (e) => http.post('/services/collector', e) });

export const logger = (sinks) => ({
  info: (msg) => {
    const e = { level: 'INFO', msg, at: new Date().toISOString() };
    sinks.forEach((s) => s.write(e));
  },
});
