// Factory Method: the base class defines the workflow and leaves ONE creation step to subclasses.
export class ReportExporter {
  export(report) {
    const writer = this.createWriter();   // ← the factory method
    writer.header(report.title);
    report.rows.forEach((r) => writer.row(r));
    return writer.finish();
  }
  createWriter() { throw new Error('subclass must implement createWriter()'); }
}

export class CsvExporter extends ReportExporter {
  createWriter() { return new CsvWriter(); }
}
export class PdfExporter extends ReportExporter {
  createWriter() { return new PdfWriter({ pageSize: 'A4' }); }
}
// In JavaScript a plain function parameter often replaces the subclass: exportReport(report, () => new CsvWriter())
