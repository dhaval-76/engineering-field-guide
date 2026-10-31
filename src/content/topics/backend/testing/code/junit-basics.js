// Vitest (Jest: same API). One fresh object per test; arrange / act / assert.
import { describe, it, expect, beforeEach } from 'vitest';
import { ShoppingCart, UnknownSkuError, EmptyCartError, bulkPercent } from './cart.js';

describe('ShoppingCart', () => {
  let cart;
  beforeEach(() => {
    cart = new ShoppingCart(new Map([['BOOK', 1250], ['PEN', 120]])); // prices in paise
  });

  it('sums line prices', () => {
    cart.add('BOOK', 2);
    cart.add('PEN', 3);
    expect(cart.total()).toBe(2860);
  });

  it('rejects an unknown sku and adds nothing', () => {
    expect(() => cart.add('LAPTOP', 1)).toThrow(UnknownSkuError);
    expect(cart.isEmpty()).toBe(true);
  });

  it.each([[1, 0], [4, 0], [5, 10], [20, 10], [21, 15]])('%i items → %i%% off', (qty, expected) => {
    expect(bulkPercent(qty)).toBe(expected);
  });

  describe('when the cart is empty', () => {
    it('has a zero total', () => expect(cart.total()).toBe(0));
    it('rejects checkout', () => expect(() => cart.checkout()).toThrow(EmptyCartError));
  });
});
