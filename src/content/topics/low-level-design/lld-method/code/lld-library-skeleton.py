# Step 2–4 output for "Design a library system": entities, relationships, the key API.
from dataclasses import dataclass, field
from datetime import date
from enum import Enum
from typing import Callable, Protocol

class CopyStatus(Enum):
    AVAILABLE = "available"
    ON_LOAN = "on_loan"
    RESERVED = "reserved"
    LOST = "lost"

@dataclass(frozen=True)
class Book:
    isbn: str
    title: str
    authors: tuple[str, ...]

@dataclass
class BookCopy:                      # a Book HAS many copies; a copy is not a Book
    barcode: str
    book: Book
    status: CopyStatus = CopyStatus.AVAILABLE
    version: int = 0                 # optimistic locking when persisted

    def mark_on_loan(self) -> None:
        if self.status is not CopyStatus.AVAILABLE:
            raise ValueError(f"{self.barcode} is {self.status.value}")
        self.status = CopyStatus.ON_LOAN

    def mark_returned(self) -> None:
        self.status = CopyStatus.AVAILABLE

@dataclass(frozen=True)
class Member:
    id: str
    name: str
    membership: str

@dataclass
class Loan:
    id: str
    member: Member
    copy: BookCopy
    due: date
    returned_on: date | None = None

    def is_overdue(self, today: date) -> bool:
        return self.returned_on is None and today > self.due

    def close(self, on: date) -> None:
        self.returned_on = on
        self.copy.mark_returned()

FinePolicy = Callable[[Loan, date], int]       # Strategy: rules vary by membership (returns paise)

class LibraryService(Protocol):                # Step 3: the operations we agreed on
    def borrow(self, member_id: str, barcode: str) -> Loan: ...
    def return_copy(self, barcode: str) -> int: ...
    def reserve(self, member_id: str, isbn: str) -> object: ...
    def search(self, query: str) -> list[Book]: ...
