import { km } from './food-order.js';

// Strategy: nearest free partner within range; ties go to the higher rating.
export const nearestAvailable = (maxKm) => (order, partners) =>
  partners
    .filter((p) => p.available && p.activeOrders === 0 && km(p.location, order.pickup) <= maxKm)
    .sort(
      (a, b) => km(a.location, order.pickup) - km(b.location, order.pickup) || b.rating - a.rating,
    )[0] ?? null;

// Listener: assign when the restaurant accepts; free the partner when the order ends.
export function dispatcher(strategy, partnersNearby) {
  const busy = new Set();
  return (order, from, to) => {
    if (to === 'ACCEPTED') {
      const p = strategy(
        order,
        partnersNearby().filter((x) => !busy.has(x.id)),
      );
      if (p) {
        busy.add(p.id);
        order.partnerId = p.id;
      }
    }
    if ((to === 'DELIVERED' || to === 'CANCELLED') && order.partnerId) busy.delete(order.partnerId);
  };
}

// Pricing strategy: ₹20 for the first 2 km, ₹8/km after, times surge, minus discount,
// never negative.
export function deliveryFeePaise(order, surge, discountPaise) {
  const base = 2000 + Math.round(Math.max(0, km(order.pickup, order.drop) - 2) * 800);
  return Math.max(0, Math.round(base * surge) - discountPaise);
}
