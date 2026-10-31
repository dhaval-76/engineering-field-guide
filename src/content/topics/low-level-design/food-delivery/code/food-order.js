// The order lifecycle as data: which moves exist and who may make them.
const RULES = [
  ['PLACED', 'ACCEPTED', ['RESTAURANT']],
  ['PLACED', 'REJECTED', ['RESTAURANT', 'SYSTEM']],
  ['PLACED', 'CANCELLED', ['CUSTOMER']],
  ['ACCEPTED', 'PREPARING', ['RESTAURANT']],
  ['ACCEPTED', 'CANCELLED', ['CUSTOMER', 'SYSTEM']],
  ['PREPARING', 'READY', ['RESTAURANT']],
  ['READY', 'PICKED_UP', ['PARTNER']],
  ['PICKED_UP', 'DELIVERED', ['PARTNER']],
];

export const allowed = (from, to, actor) => RULES.some(([f, t, who]) => f === from && t === to && who.includes(actor));

export function km(a, b) {
  const rad = (d) => (d * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}

export class Order {
  status = 'PLACED';
  partnerId = null;
  history = ['PLACED'];

  constructor({ id, restaurantId, pickup, drop, totalPaise }, listeners = []) {
    Object.assign(this, { id, restaurantId, pickup, drop, totalPaise });
    this.listeners = listeners;
  }

  moveTo(next, actor) {
    if (!allowed(this.status, next, actor)) throw new Error(`${actor} cannot move ${this.id} from ${this.status} to ${next}`);
    if (next === 'PICKED_UP' && !this.partnerId) throw new Error('no partner assigned');
    const prev = this.status;
    this.status = next;
    this.history.push(next);
    for (const l of this.listeners) l(this, prev, next);
  }
}
