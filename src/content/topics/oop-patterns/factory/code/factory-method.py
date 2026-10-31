from abc import ABC, abstractmethod

class ReportExporter(ABC):
    """Factory Method: the base class defines the workflow and leaves ONE creation step to subclasses."""

    def export(self, report) -> bytes:
        writer = self.create_writer()             # ← the factory method
        writer.header(report.title)
        for row in report.rows:
            writer.row(row)
        return writer.finish()

    @abstractmethod
    def create_writer(self): ...

class CsvExporter(ReportExporter):
    def create_writer(self):
        return CsvWriter()

class PdfExporter(ReportExporter):
    def create_writer(self):
        return PdfWriter(page_size="A4")
# In Python a callable parameter often replaces the subclass: export_report(report, writer_factory=CsvWriter)
