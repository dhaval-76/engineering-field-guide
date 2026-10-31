// MongoDB Node driver. An order is an aggregate: items and address embedded, the customer
// referenced. { _id: 'ord_8f2c', customerId: 'cus_42', status: 'PLACED',
// items: [{ productId: 'prd_1', name: 'Air 14', qty: 1, unitPrice: Decimal128('999.00')
// }, …],
//   shipping: { line1: '12 MG Road', city: 'Pune', postcode: '411001' }, createdAt: ISODate(…) }
// Reviews are NOT embedded in products: unbounded and written independently → their own collection.
const orders = db.collection('orders');
// equality on customer, then sort by time
await orders.createIndex({ customerId: 1, createdAt: -1 });

// Uses the compound index
const recent = await orders.find({ customerId }).sort({ createdAt: -1 }).limit(20).toArray();

// Matches inside the embedded array; add an index on items.productId if this is hot
const containing = await orders.find({ 'items.productId': productId }).toArray();

// Atomic in-place update of one document — no read-modify-write in application code
await orders.updateOne({ _id: orderId, status: 'PLACED' }, { $set: { status: 'PAID' } });
