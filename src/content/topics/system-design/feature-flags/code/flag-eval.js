import { createHash } from 'node:crypto';

const BUCKETS = 100_000;

// The same user always gets the same bucket for a flag, in every SDK language.
export function bucket(flagKey, salt, userId) {
  const h = createHash('sha256').update(`${flagKey}:${salt}:${userId}`, 'utf8').digest();
  return h.readUInt32BE(0) % BUCKETS;
}

// flag: { key, on, salt, offVariation, targets: {userId: variation},
//         rules: [{ attribute, values: [...], variation }],
//         rollout: [{ variation, weight }] }  (weights out of 100,000)
export function evaluate(flag, userId, attrs = {}) {
  if (!flag.on) return { variation: flag.offVariation, reason: 'OFF' };
  const target = flag.targets[userId];
  if (target !== undefined) return { variation: target, reason: 'TARGET' };
  for (const [i, r] of flag.rules.entries()) {
    if (r.values.includes(attrs[r.attribute])) return { variation: r.variation, reason: `RULE ${i}` };
  }
  // Walk the slices: bucket 0..99,999 falls into exactly one of them.
  const b = bucket(flag.key, flag.salt, userId);
  let upTo = 0;
  for (const s of flag.rollout) {
    upTo += s.weight;
    if (b < upTo) return { variation: s.variation, reason: 'ROLLOUT' };
  }
  return { variation: flag.offVariation, reason: 'ROLLOUT' };
}
