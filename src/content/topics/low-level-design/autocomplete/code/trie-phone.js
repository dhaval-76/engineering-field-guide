// Digits-only trie with contacts stored at the end node; prefix search returns contacts in digit order.
export class PhoneDirectory {
  #root = { next: new Array(10).fill(null), contacts: [] };

  add(contact) {
    let n = this.#root;
    for (const d of contact.number) {
      const i = d.charCodeAt(0) - 48;
      if (i < 0 || i > 9) throw new Error(`digits only: ${contact.number}`);
      n = n.next[i] ??= { next: new Array(10).fill(null), contacts: [] };
    }
    n.contacts.push(contact);
  }

  byPrefix(digits, limit) {
    let n = this.#root;
    for (const d of digits) {
      n = n.next[d.charCodeAt(0) - 48];
      if (!n) return [];
    }
    const out = [];
    const stack = [n];
    while (stack.length && out.length < limit) {
      const cur = stack.pop();
      out.push(...cur.contacts);
      for (let i = 9; i >= 0; i--) if (cur.next[i]) stack.push(cur.next[i]); // so 0 is visited first
    }
    return out.slice(0, limit);
  }
}
