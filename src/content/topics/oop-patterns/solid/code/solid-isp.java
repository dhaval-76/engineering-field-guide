// ✗ A fat interface forces fake implementations.
interface Printer { void print(Doc d); void scan(Doc d); void fax(Doc d); }
class BasicPrinter implements Printer {
    public void print(Doc d) { /* works */ }
    public void scan(Doc d)  { throw new UnsupportedOperationException(); }
    public void fax(Doc d)   { throw new UnsupportedOperationException(); }
}

// ✓ Small role interfaces; classes implement only what they really do.
interface Printable { void print(Doc d); }
interface Scannable { void scan(Doc d); }
interface Faxable   { void fax(Doc d); }

class BasicPrinter implements Printable { public void print(Doc d) { } }
class OfficeMachine implements Printable, Scannable, Faxable { /* all three */ }

// Clients depend on the slice they use:
class ReportJob { ReportJob(Printable printer) { /* only ever prints */ } }
