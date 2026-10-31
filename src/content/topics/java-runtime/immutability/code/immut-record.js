// Object.freeze is shallow; freeze nested objects too, or treat data as immutable by
// convention.
const money = Object.freeze({ minorUnits: 500, currency: 'INR' });
money.minorUnits = 0;            // ignored (throws in strict mode)

const plus = (a, b) => {
  if (a.currency !== b.currency) throw new Error('currency mismatch');
  return Object.freeze({ ...a, minorUnits: a.minorUnits + b.minorUnits });   // new object
};

// Non-mutating array methods (ES2023) return copies:
const sorted = items.toSorted((x, y) => x.price - y.price);   // items unchanged
const without = items.toSpliced(2, 1);
