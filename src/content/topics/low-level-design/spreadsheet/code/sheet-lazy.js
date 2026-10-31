// uses parse() from above Alternative: mark dependents dirty on write, recompute on read
// (good when most cells are never viewed).
export class LazySheet {
  #cells = new Map(); // id -> { expr, value }
  #dependents = new Map(); // id -> Set of readers
  #dirty = new Set();

  setFormula(id, text) {
    const expr = parse(text);
    for (const r of expr.refs) {
      if (!this.#dependents.has(r)) this.#dependents.set(r, new Set());
      this.#dependents.get(r).add(id);
    }
    this.#cells.set(id, { expr, value: 0 });
    this.#markDirty(id);
  }

  #markDirty(id) {
    const stack = [id];
    while (stack.length) {
      const c = stack.pop();
      if (this.#dirty.has(c)) continue;
      this.#dirty.add(c);
      stack.push(...(this.#dependents.get(c) ?? []));
    }
  }

  get = (id) => {
    const c = this.#cells.get(id);
    if (!c) return 0;
    // recursion pulls dirty inputs first
    if (this.#dirty.delete(id)) c.value = c.expr.eval(this.get);
    return c.value;
  };
}
// (Dependents of a replaced formula are not unlinked here, which only causes extra dirty
// marks.)
