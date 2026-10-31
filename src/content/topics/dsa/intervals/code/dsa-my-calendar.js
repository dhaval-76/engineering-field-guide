// Bookings arrive one at a time as half-open [start, end). Reject one that overlaps any
// booking. JavaScript has no sorted map; keep bookings sorted by start and binary-search
// the insertion point. Only the neighbours on either side can clash. O(log n) search +
// O(n) insert.
class MyCalendar {
  #booked = []; // [start, end], sorted by start

  book(start, end) {
    let lo = 0;
    let hi = this.#booked.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (this.#booked[mid][0] < start) lo = mid + 1;
      else hi = mid;
    }
    const before = this.#booked[lo - 1]; // starts before
    const after = this.#booked[lo]; // starts at or after
    if (before && before[1] > start) return false; // still running
    if (after && after[0] < end) return false; // starts too soon
    this.#booked.splice(lo, 0, [start, end]);
    return true;
  }
}
// book(10,20) true, book(15,25) false, book(20,30) true (touching is fine).
