// Prices are integer ticks (e.g. paise), never floating point — 0.1 + 0.2 must equal 0.3.
export const newOrder = ({ id, userId, symbol, side, type = 'LIMIT', limit = null, qty }) => {
  if (!['BUY', 'SELL'].includes(side)) throw new Error(`bad side ${side}`);
  if (type === 'LIMIT' && !Number.isInteger(limit)) throw new Error('limit orders need an integer tick price');
  if (!(Number.isInteger(qty) && qty > 0)) throw new Error('qty must be a positive integer');
  // seq assigned by the book
  return { id, userId, symbol, side, type, limit, qty, remaining: qty, status: 'OPEN' };
};
// trade: { symbol, buy: orderId, sell: orderId, price: ticks, qty, seq }
