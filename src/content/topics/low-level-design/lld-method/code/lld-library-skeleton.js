// Step 2–4 output for "Design a library system": entities, relationships, the key API.
// book: { isbn, title, authors }   member: { id, name, type }

export class BookCopy {             // a Book HAS many copies; a copy is not a Book
  status = 'AVAILABLE';             // AVAILABLE | ON_LOAN | RESERVED | LOST
  version = 0;                      // optimistic locking when persisted
  constructor(barcode, book) {
    Object.assign(this, { barcode, book });
  }
  get isAvailable() {
    return this.status === 'AVAILABLE';
  }
  markOnLoan() {
    if (!this.isAvailable) throw new Error(`${this.barcode} is ${this.status}`);
    this.status = 'ON_LOAN';
  }
  markReturned() {
    this.status = 'AVAILABLE';
  }
}

export class Loan {
  returnedOn = null;
  constructor(id, member, copy, due) {
    // dates as 'YYYY-MM-DD' strings compare correctly
    Object.assign(this, { id, member, copy, due });
  }
  isOverdue(today) {
    return this.returnedOn === null && today > this.due;
  }
  close(on) {
    this.returnedOn = on;
    this.copy.markReturned();
  }
}

// Strategy: fine rules vary by membership — finePolicy(loan, returnedOn) → paise
// LibraryService (Step 3, the operations we agreed on):
//   borrow(memberId, barcode) → Loan
//   returnCopy(barcode) → fine in paise
//   reserve(memberId, isbn) → Reservation
//   search(query) → Book[]
