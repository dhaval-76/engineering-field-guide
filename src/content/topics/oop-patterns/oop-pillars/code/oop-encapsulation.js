export class BankAccount {
  #balancePaise; // `#` = truly private field (not reachable from outside the class)

  constructor(accountNumber, openingPaise) {
    if (openingPaise < 0) throw new RangeError('opening balance < 0');
    this.accountNumber = accountNumber;
    this.#balancePaise = openingPaise;
  }

  deposit(paise) {
    if (paise <= 0) throw new RangeError('deposit must be positive');
    this.#balancePaise += paise;
  }

  withdraw(paise) {
    if (paise <= 0) throw new RangeError('withdrawal must be positive');
    if (paise > this.#balancePaise) throw new Error('insufficient funds');
    this.#balancePaise -= paise;
  }

  get balance() {
    return this.#balancePaise;
  }
}
