// BigInt: JavaScript numbers lose precision above 2^53, so 64-bit ids must be BigInt
// (and strings in JSON).
const EPOCH = BigInt(Date.parse('2024-01-01T00:00:00Z'));
const MAX_SEQUENCE = 4095n;

export function snowflake(worker) {
  if (worker < 0 || worker > 1023) throw new RangeError('worker 0–1023');
  const w = BigInt(worker);
  let lastMs = -1n;
  let sequence = 0n;

  return function nextId() {               // single-threaded event loop: no locking needed
    let now = BigInt(Date.now());
    if (now < lastMs) {
      if (lastMs - now > 10n) throw new Error(`clock moved back ${lastMs - now} ms`);
      while ((now = BigInt(Date.now())) < lastMs);
    }
    if (now === lastMs) {
      sequence = (sequence + 1n) & MAX_SEQUENCE;
      if (sequence === 0n) while ((now = BigInt(Date.now())) <= lastMs);
    } else {
      sequence = 0n;
    }
    lastMs = now;
    return ((now - EPOCH) << 22n) | (w << 12n) | sequence;
  };
}

const next = snowflake(7);
console.log(next().toString());            // send as a string
