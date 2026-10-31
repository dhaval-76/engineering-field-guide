// Amounts in minor units (paise, cents); dates as day numbers.
const DATE_WINDOW = 2; // days a settlement may lag or lead
const TRANSIT_DAYS = 3; // how long an unmatched payment may wait

// ours: [{ id, ref, amount, currency, day }]; theirs: [{ lineId, ref, amount, currency, day }]
export function reconcile(ours, theirs, today) {
  const out = [];
  const byRef = new Map(ours.map((p) => [p.ref, p]));
  const matched = new Set();
  const leftover = [];

  // Pass 1: the provider echoed our reference, so match on it exactly.
  for (const l of theirs) {
    const p = byRef.get(l.ref);
    if (!p) {
      leftover.push(l);
    } else if (matched.has(p.id)) {
      out.push({ status: 'DUPLICATE', paymentId: p.id, lineId: l.lineId });
    } else {
      matched.add(p.id);
      const same = p.amount === l.amount && p.currency === l.currency;
      out.push({ status: same ? 'MATCHED' : 'AMOUNT_MISMATCH', paymentId: p.id, lineId: l.lineId });
    }
  }

  // Pass 2: no usable reference; pair by amount and currency, closest date first.
  const open = new Map();
  for (const p of ours) {
    if (matched.has(p.id)) continue;
    const key = p.amount + p.currency;
    if (!open.has(key)) open.set(key, []);
    open.get(key).push(p);
  }
  for (const l of leftover) {
    const candidates = open.get(l.amount + l.currency) ?? [];
    let best = null;
    for (const p of candidates) {
      const gap = Math.abs(p.day - l.day);
      if (gap <= DATE_WINDOW && (best === null || gap < Math.abs(best.day - l.day))) best = p;
    }
    if (best === null) {
      out.push({ status: 'MISSING_IN_LEDGER', paymentId: null, lineId: l.lineId });
    } else {
      candidates.splice(candidates.indexOf(best), 1);
      matched.add(best.id);
      out.push({ status: 'MATCHED_BY_AMOUNT', paymentId: best.id, lineId: l.lineId });
    }
  }

  // Whatever is left on our side is either still in transit or a real break.
  for (const p of ours) {
    if (matched.has(p.id)) continue;
    const status = today - p.day <= TRANSIT_DAYS ? 'IN_TRANSIT' : 'MISSING_AT_PROVIDER';
    out.push({ status, paymentId: p.id, lineId: null });
  }
  return out;
}
