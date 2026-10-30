// Same algorithm in JS, with formulas compiled to functions of a `get` callback.
export class Sheet {
  cells = new Map();       // id -> { refs: Set, fn: (get) => number, value }
  dependents = new Map();  // id -> Set of ids that read it

  set(id, refs, fn) {
    if (this.#createsCycle(id, refs)) throw new Error(`circular reference at ${id}`);
    const old = this.cells.get(id)?.refs ?? new Set();
    old.forEach((r) => this.dependents.get(r)?.delete(id));
    refs.forEach((r) => (this.dependents.get(r) ?? this.dependents.set(r, new Set()).get(r)).add(id));
    this.cells.set(id, { refs: new Set(refs), fn, value: 0 });
    this.#recalc(id);
  }

  get = (id) => this.cells.get(id)?.value ?? 0;

  #createsCycle(id, refs) {
    const stack = [...refs], seen = new Set();
    while (stack.length) {
      const cur = stack.pop();
      if (cur === id) return true;
      if (seen.has(cur)) continue;
      seen.add(cur);
      stack.push(...(this.cells.get(cur)?.refs ?? []));
    }
    return false;
  }

  #recalc(start) {
    const affected = new Set(), stack = [start];
    while (stack.length) {
      const c = stack.pop();
      if (!affected.has(c)) { affected.add(c); stack.push(...(this.dependents.get(c) ?? [])); }
    }
    const indeg = new Map([...affected].map((c) => [c, 0]));
    for (const c of affected) for (const d of this.dependents.get(c) ?? []) if (affected.has(d)) indeg.set(d, indeg.get(d) + 1);
    const ready = [...affected].filter((c) => indeg.get(c) === 0);
    while (ready.length) {
      const c = ready.shift();
      const cell = this.cells.get(c);
      if (cell) cell.value = cell.fn(this.get);
      for (const d of this.dependents.get(c) ?? []) {
        if (affected.has(d)) { indeg.set(d, indeg.get(d) - 1); if (indeg.get(d) === 0) ready.push(d); }
      }
    }
  }
}

// const s = new Sheet();
// s.set('A1', [], () => 10);
// s.set('B1', ['A1'], (g) => g('A1') * 2);
// s.set('C1', ['A1', 'B1'], (g) => g('A1') + g('B1'));   // 30
