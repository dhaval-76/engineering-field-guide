import { BankAccount } from './bank-account.js';

export class SavingsAccount extends BankAccount {
  #annualRate;
  #withdrawalsLeft = 3;

  constructor(number, openingPaise, annualRate) {
    super(number, openingPaise);
    this.#annualRate = annualRate;
  }

  applyMonthlyInterest() {
    this.deposit(Math.round((this.balance * this.#annualRate) / 12));
  }

  withdraw(paise) {
    if (this.#withdrawalsLeft === 0) throw new Error('monthly limit reached');
    super.withdraw(paise);
    this.#withdrawalsLeft--;
  }
}
