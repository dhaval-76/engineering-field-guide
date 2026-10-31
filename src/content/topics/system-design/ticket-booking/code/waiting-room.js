// Virtual waiting room: a place in line for everyone, admission at a controlled rate,
// expiring sessions.
export class WaitingRoom {
  #seqOf = new Map(); // user -> place in line
  #admittedAt = new Map(); // seq -> time admitted
  #nextSeq = 0;
  #admittedUpTo = 0; // seq < admittedUpTo are admitted

  constructor(admitPerTick, sessionMs) {
    this.admitPerTick = admitPerTick;
    this.sessionMs = sessionMs;
  }

  join(userId) {
    // refresh keeps your place
    if (!this.#seqOf.has(userId)) this.#seqOf.set(userId, this.#nextSeq++);
    return this.#seqOf.get(userId);
  }

  tick(now) {
    const upTo = Math.min(this.#nextSeq, this.#admittedUpTo + this.admitPerTick);
    for (let s = this.#admittedUpTo; s < upTo; s++) this.#admittedAt.set(s, now);
    this.#admittedUpTo = upTo;
  }

  position(userId) {
    return Math.max(0, this.#seqOf.get(userId) - this.#admittedUpTo + 1);
  }

  mayBook(userId, now) {
    const seq = this.#seqOf.get(userId);
    if (seq === undefined || seq >= this.#admittedUpTo) return false;
    return now - this.#admittedAt.get(seq) <= this.sessionMs;
  }
}
