// JavaScript has no equals/hashCode hooks: Map and Set compare object keys by identity.
const a = { id: 1 };
const b = { id: 1 };
new Set([a, b]).size;            // 2 — different objects

// Use a value as the key instead (string, number) — the usual pattern:
const byId = new Map();
byId.set(`${order.tenant}:${order.id}`, order);

// Value equality must be written explicitly:
const sameMoney = (m1, m2) => m1.amountMinor === m2.amountMinor && m1.currency === m2.currency;
