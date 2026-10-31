// Each state object decides what a button means and which state comes next.
const idle = {
  insertCoin: (m, paise) => { m.credit += paise; m.state = hasCredit; },
  select: (m) => m.display('Insert money first'),
  cancel: () => {},
};

const hasCredit = {
  insertCoin: (m, paise) => { m.credit += paise; },
  select: (m, code) => {
    const p = m.products.get(code);
    if (!p || p.stock === 0) return m.display('Unavailable');
    if (m.credit < p.price) return m.display(`Insert ${p.price - m.credit} more`);
    m.state = dispensing;
    m.dispense(p); // dispensing → idle when done, returns change
  },
  cancel: (m) => { m.refund(m.credit); m.credit = 0; m.state = idle; },
};

const dispensing = {
  insertCoin: (m, paise) => m.refund(paise),
  select: (m) => m.display('Please wait'),
  cancel: () => {},
};

export class VendingMachine {
  state = idle;
  credit = 0;
  constructor(products, io) {
    Object.assign(this, { products, ...io }); // io: { display, refund, release }
  }
  // no switch on an enum anywhere
  insertCoin(paise) { this.state.insertCoin(this, paise); }
  select(code) { this.state.select(this, code); }
  cancel() { this.state.cancel(this); }
  dispense(p) {
    p.stock--;
    this.release(p);
    this.refund(this.credit - p.price);
    this.credit = 0;
    this.state = idle;
  }
}
