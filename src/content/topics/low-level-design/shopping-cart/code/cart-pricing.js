// The same pipeline with plain functions: each promotion returns an adjustment or null.
const percentOffAbove = (threshold, pct) => ({ group: 'ORDER_PERCENT', evaluate: (lines, total) => (total >= threshold ? { label: `${pct}% off`, amount: -Math.floor((total * pct) / 100) } : null) });
const flatCoupon = (code, off, min = 0) => ({ group: 'COUPON', evaluate: (lines, total) => (total >= min ? { label: `Coupon ${code}`, amount: -Math.min(off, total) } : null) });

export function price(lines, promotions, taxRate) {
  let total = lines.reduce((s, l) => s + l.unitPaise * l.qty, 0);
  const applied = [];
  for (const group of ['ITEM', 'ORDER_PERCENT', 'COUPON']) {
    const offers = promotions.filter((p) => p.group === group).map((p) => p.evaluate(lines, total)).filter(Boolean);
    const best = offers.sort((a, b) => a.amount - b.amount)[0];
    if (best) { applied.push(best); total += best.amount; }
  }
  const tax = Math.round(total * taxRate);
  return { applied, tax, payable: total + tax };
}

// price(lines, [percentOffAbove(1_000_000, 10), flatCoupon('SAVE500', 50_000)], 0.18)
