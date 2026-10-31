import { randomUUID } from 'node:crypto';

// Strategy: choose a warehouse that can fill the whole quantity (most stock first, then alphabetical).
export const mostStockFirst = (sku, qty, available) =>
  [...available.entries()]
    .filter(([, n]) => n >= qty)
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))[0]?.[0] ?? null;

export class InventoryService {
  #levels = new Map(); // "warehouse|sku" -> { warehouse, sku, onHand, reserved }
  #reservations = new Map();
  #reorderPoint = new Map();
  #listeners = [];
  movements = [];

  constructor(strategy = mostStockFirst) {
    this.strategy = strategy;
  }

  receive(warehouse, sku, qty) {
    this.#level(warehouse, sku).onHand += qty;
    this.movements.push(`RECEIVE ${warehouse} ${sku} +${qty}`);
  }

  reserve(orderId, sku, qty, now, ttlMs) {
    this.#expire(now);
    const available = new Map();
    for (const l of this.#levels.values()) if (l.sku === sku) available.set(l.warehouse, l.onHand - l.reserved);
    const warehouse = this.strategy(sku, qty, available);
    if (!warehouse) return null; // never oversell
    this.#level(warehouse, sku).reserved += qty;
    const r = { id: randomUUID(), orderId, warehouse, sku, qty, expiresAt: now + ttlMs };
    this.#reservations.set(r.id, r);
    this.movements.push(`RESERVE ${warehouse} ${sku} ${qty} for ${orderId}`);
    this.#checkReorder(sku);
    return r;
  }

  confirm(reservationId, now) {
    this.#expire(now);
    const r = this.#reservations.get(reservationId);
    if (!r) return false;
    this.#reservations.delete(reservationId);
    const l = this.#level(r.warehouse, r.sku);
    l.reserved -= r.qty;
    l.onHand -= r.qty;
    this.movements.push(`SHIP ${r.warehouse} ${r.sku} -${r.qty} for ${r.orderId}`);
    return true;
  }

  cancel(reservationId) {
    const r = this.#reservations.get(reservationId);
    if (r) this.#release(r, 'CANCEL');
  }

  available(sku) {
    let n = 0;
    for (const l of this.#levels.values()) if (l.sku === sku) n += l.onHand - l.reserved;
    return n;
  }

  setReorderPoint(sku, point) {
    this.#reorderPoint.set(sku, point);
  }
  onLowStock(listener) {
    this.#listeners.push(listener);
  }

  #expire(now) {
    for (const r of [...this.#reservations.values()]) if (r.expiresAt <= now) this.#release(r, 'EXPIRE');
  }

  #release(r, why) {
    this.#reservations.delete(r.id);
    this.#level(r.warehouse, r.sku).reserved -= r.qty;
    this.movements.push(`${why} ${r.warehouse} ${r.sku} ${r.qty} for ${r.orderId}`);
  }

  #checkReorder(sku) {
    const point = this.#reorderPoint.get(sku);
    const total = this.available(sku);
    if (point !== undefined && total <= point) this.#listeners.forEach((fn) => fn(sku, total));
  }

  #level(warehouse, sku) {
    const key = `${warehouse}|${sku}`;
    if (!this.#levels.has(key)) this.#levels.set(key, { warehouse, sku, onHand: 0, reserved: 0 });
    return this.#levels.get(key);
  }
}
