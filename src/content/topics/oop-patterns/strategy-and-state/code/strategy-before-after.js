// Functions are first-class, so a strategy is often just a function.
const shippingPolicies = {
  STANDARD: (order) => order.weightKg * 40,
  EXPRESS: (order) => order.weightKg * 90 + 50,
  PICKUP: () => 0,
};

export function total(order, option) {
  const cost = shippingPolicies[option];
  if (!cost) throw new Error(`unknown shipping option ${option}`);
  return order.subtotal + cost(order);
}
