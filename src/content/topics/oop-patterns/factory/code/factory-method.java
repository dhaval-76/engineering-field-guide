// Factory Method: the base class defines the workflow and leaves ONE creation step to
// subclasses.
abstract class ReportExporter {
    public final byte[] export(Report report) {        // template of the algorithm
        Writer writer = createWriter();                // ← the factory method
        writer.header(report.title());
        report.rows().forEach(writer::row);
        return writer.finish();
    }
    protected abstract Writer createWriter();
}

class CsvExporter extends ReportExporter {
    protected Writer createWriter() { return new CsvWriter(); }
}
class PdfExporter extends ReportExporter {
    protected Writer createWriter() { return new PdfWriter(PageSize.A4); }
}
