// A registry of strategies: adding a discount registers a function; price() never changes.
const discounts = new Map([
  ['FLAT', (order) => order.total - 100],
  ['PERCENT', (order) => order.total * 0.9],
]);

export const registerDiscount = (name, fn) => discounts.set(name, fn);

export function price(order, type) {
  const apply = discounts.get(type);
  if (!apply) throw new Error(`unknown discount ${type}`);
  return apply(order);
}

registerDiscount('LOYALTY', (order) => order.total * 0.95);
