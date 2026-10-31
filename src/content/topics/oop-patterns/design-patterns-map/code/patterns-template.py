from abc import ABC, abstractmethod

class DataImport(ABC):
    """Template Method: the base class fixes the steps; subclasses fill in some of them."""

    def run(self, file) -> dict:              # the order of steps lives here only
        rows = self.parse(file)
        valid = [r for r in rows if self.is_valid(r)]
        self.save(valid)
        return {"total": len(rows), "imported": len(valid)}

    @abstractmethod
    def parse(self, file) -> list[dict]: ...  # varies: CSV, Excel, JSON

    def is_valid(self, row: dict) -> bool:    # optional hook
        return row.get("id") is not None

    @abstractmethod
    def save(self, rows: list[dict]) -> None: ...
