// Template Method: the base class fixes the steps; subclasses fill in some of them.
export class DataImport {
  run(file) {                          // the order of steps lives here only
    const rows = this.parse(file);
    const valid = rows.filter((r) => this.isValid(r));
    this.save(valid);
    return { total: rows.length, imported: valid.length };
  }
  // varies: CSV, Excel, JSON
  parse(file) { throw new Error('subclass must implement parse()'); }
  isValid(row) { return row.id != null; }                                // optional hook
  save(rows) { throw new Error('subclass must implement save()'); }
}
