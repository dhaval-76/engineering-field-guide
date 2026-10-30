// JS strings are immutable too. Engines optimise += with "ropes", so small loops are fine,
// but for large outputs building an array and joining once is predictable:
const parts = [];
for (const r of rows) parts.push(r.toCsv());
const csv = parts.join('\n');

// Streaming large outputs avoids holding everything in memory at all:
// for (const r of rows) res.write(r.toCsv() + '\n');
