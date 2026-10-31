// Node: a Map is safe within one process (no two callbacks run at once), so no locks are
// needed here.
export class Library {
  #checkedOut = new Map(); // bookId → userId

  checkout(userId, bookId) {
    // check and set happen with no await between
    if (this.#checkedOut.has(bookId)) return false;
    this.#checkedOut.set(bookId, userId);
    return true;
  }

  returnBook(userId, bookId) {
    // only the holder can return it
    if (this.#checkedOut.get(bookId) !== userId) return false;
    this.#checkedOut.delete(bookId);
    return true;
  }
}
// Deadlocks appear in JS too once work spans awaits and several resources (two async
// mutexes taken in different orders) — the same fix applies: acquire in a global order.
// Multi-instance: UPDATE books SET holder = $1 WHERE id = $2 AND holder IS NULL  → 1 row
// = success.
