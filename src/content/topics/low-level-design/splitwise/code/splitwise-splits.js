// Amounts are integers in the smallest currency unit (paise). Each strategy returns shares summing exactly to total.
export const equalSplit = (total, people) => {
  const base = Math.floor(total / people.length), remainder = total % people.length;
  return new Map(people.map((p, i) => [p, base + (i < remainder ? 1 : 0)]));   // spread leftover paise deterministically
};

export const exactSplit = (amounts) => (total) => {
  const sum = [...amounts.values()].reduce((a, b) => a + b, 0);
  if (sum !== total) throw new Error(`exact amounts add up to ${sum}, not ${total}`);
  return new Map(amounts);
};

// Percentages as integer basis points (33.34% = 3334) so the arithmetic stays exact.
export const percentSplit = (basisPoints) => {
  const sum = [...basisPoints.values()].reduce((a, b) => a + b, 0);
  if (sum !== 10000) throw new Error('percentages must add up to 100');
  return (total, people) => {
    let assigned = 0;
    return new Map(people.map((p, i) => {
      const share = i === people.length - 1
        ? total - assigned                                           // last person absorbs rounding
        : Math.floor((basisPoints.get(p) * total + 5000) / 10000);   // round half up
      assigned += share;
      return [p, share];
    }));
  };
};
