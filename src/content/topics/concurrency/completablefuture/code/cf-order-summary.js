// Enrich every item in parallel; one bad item never fails the whole response.
export async function summarize(order, { pricing, inventory }) {
  return Promise.all(order.items.map(async (item) => {
    try {
      const [price, inStock] = await Promise.all([
        pricing.priceOf(item.sku, { signal: AbortSignal.timeout(300) }),
        inventory.inStock(item.sku, item.qty, { signal: AbortSignal.timeout(300) }),
      ]);
      return inStock ? { item, price, status: 'AVAILABLE' } : { item, price: null, status: 'UNAVAILABLE' };
    } catch {
      return { item, price: null, status: 'UNAVAILABLE' };
    }
  }));
}
