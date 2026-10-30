// Promises are the JavaScript equivalent: start all three, then await them together.
async function buildPage(productId) {
  const withTimeout = (p, ms) =>
    Promise.race([p, new Promise((_, rej) => setTimeout(() => rej(new Error('timeout')), ms))]);

  const [product, stock, price] = await Promise.all([
    withTimeout(productService.get(productId), 300),
    withTimeout(inventoryService.get(productId), 300).catch(() => Inventory.UNKNOWN),   // fallback
    withTimeout(pricingService.get(productId), 300),
  ]);
  return { product, stock, price };
}
// Promise.allSettled gives every result (fulfilled or rejected) instead of failing fast.
