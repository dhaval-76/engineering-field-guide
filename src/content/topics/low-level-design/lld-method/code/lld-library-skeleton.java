// Step 2–4 output for "Design a library system": entities, relationships, the key API.
enum CopyStatus {
    AVAILABLE,
    ON_LOAN,
    RESERVED,
    LOST
}

record Book(String isbn, String title, List<String> authors) {}

// a Book HAS many copies; a copy is not a Book
final class BookCopy {
    private final String barcode;
    private final Book book;
    private CopyStatus status = CopyStatus.AVAILABLE;
    private long version; // optimistic locking when persisted

    BookCopy(String barcode, Book book) {
        this.barcode = barcode;
        this.book = book;
    }

    boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }

    void markOnLoan() {
        if (!isAvailable()) throw new IllegalStateException(barcode + " is " + status);
        status = CopyStatus.ON_LOAN;
    }

    void markReturned() {
        status = CopyStatus.AVAILABLE;
    }
}

record Member(String id, String name, MembershipType type) {}

final class Loan {
    private final String id;
    private final Member member;
    private final BookCopy copy;
    private final LocalDate due;
    private LocalDate returnedOn;

    Loan(String id, Member m, BookCopy c, LocalDate due) {
        this.id = id;
        member = m;
        copy = c;
        this.due = due;
    }

    boolean isOverdue(LocalDate today) {
        return returnedOn == null && today.isAfter(due);
    }

    void close(LocalDate on) {
        returnedOn = on;
        copy.markReturned();
    }
}

// Strategy: rules vary by membership
interface FinePolicy {
    Money fineFor(Loan loan, LocalDate returnedOn);
}

interface LibraryService { // Step 3: the operations we agreed on
    Loan borrow(String memberId, String barcode);

    Money returnCopy(String barcode);

    Reservation reserve(String memberId, String isbn);

    List<Book> search(String query);
}
