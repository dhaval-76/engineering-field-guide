// Template Method: the base class fixes the steps; subclasses fill in some of them.
abstract class DataImport {
    public final ImportReport run(Path file) {            // final: the order of steps can't change
        List<Row> rows = parse(file);
        List<Row> valid = rows.stream().filter(this::isValid).toList();
        save(valid);
        return new ImportReport(rows.size(), valid.size());
    }
    protected abstract List<Row> parse(Path file);          // varies: CSV, Excel, JSON
    protected boolean isValid(Row r) { return r.hasRequiredFields(); }   // optional hook
    protected abstract void save(List<Row> rows);
}
