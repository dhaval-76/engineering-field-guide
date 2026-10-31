// Double-entry wallet. Node runs this on one thread, so each method is atomic as long as it
// doesn't await in the middle; with a database, the same rules become SQL (see wallet-schema).
import { randomUUID } from 'node:crypto';

export class InsufficientFunds extends Error {}

export class WalletService {
  #accounts = new Map(); // id -> { balance, held }
  #idem = new Map();
  #holds = new Map(); // holdId -> { account, amount }
  ledger = []; // { txnId, account, amount }

  open(id) {
    if (!this.#accounts.has(id)) this.#accounts.set(id, { balance: 0, held: 0 });
  }
  balance(id) {
    return this.#accounts.get(id).balance;
  }
  available(id) {
    const a = this.#accounts.get(id);
    return a.balance - a.held;
  }

  deposit(id, amount, key) {
    this.open('system:bank-in');
    return this.#transfer('system:bank-in', id, amount, key, { allowNegative: true });
  }

  transfer(from, to, amount, key) {
    return this.#transfer(from, to, amount, key, {});
  }

  #transfer(from, to, amount, key, { allowNegative = false, releaseHeld = 0 }) {
    if (!(amount > 0) || from === to || !Number.isInteger(amount)) throw new Error('invalid transfer');
    if (this.#idem.has(key)) return this.#idem.get(key);
    const a = this.#accounts.get(from);
    const b = this.#accounts.get(to);
    if (!allowNegative && a.balance - a.held + releaseHeld < amount) throw new InsufficientFunds(from);
    const txnId = randomUUID();
    a.held -= releaseHeld;
    a.balance -= amount;
    b.balance += amount;
    this.ledger.push({ txnId, account: from, amount: -amount }, { txnId, account: to, amount });
    this.#idem.set(key, txnId);
    return txnId;
  }

  hold(id, amount) {
    if (this.available(id) < amount) throw new InsufficientFunds(id);
    this.#accounts.get(id).held += amount;
    const holdId = randomUUID();
    this.#holds.set(holdId, { account: id, amount });
    return holdId;
  }

  capture(holdId, merchant, amount, key) {
    const h = this.#holds.get(holdId);
    if (!h) throw new Error('unknown or finished hold');
    if (amount > h.amount) throw new Error('capture exceeds hold');
    const txnId = this.#transfer(h.account, merchant, amount, key, { releaseHeld: h.amount });
    this.#holds.delete(holdId);
    return txnId;
  }

  release(holdId) {
    const h = this.#holds.get(holdId);
    if (!h) return;
    this.#accounts.get(h.account).held -= h.amount;
    this.#holds.delete(holdId);
  }
}
