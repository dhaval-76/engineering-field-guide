import { randomUUID } from 'node:crypto';

// Net balance per member: + is owed money, − owes money. Always sums to zero.
export class Group {
  expenses = [];
  #net = new Map();

  constructor(id) {
    this.id = id;
  }

  addExpense(paidBy, amount, participants, split, description) {
    const owed = split(amount, participants);
    const e = { id: randomUUID(), paidBy, amount, owed, description };
    this.expenses.push(e);
    this.#add(paidBy, amount);                                 // payer fronted the whole amount
    for (const [person, share] of owed) this.#add(person, -share);
    return e;
  }

  recordPayment(from, to, amount) {                            // a settle-up is just a transfer
    this.#add(from, amount);
    this.#add(to, -amount);
  }

  netBalances() {
    return new Map(this.#net);
  }

  #add(person, delta) {
    this.#net.set(person, (this.#net.get(person) ?? 0) + delta);
  }
}
