interface VendingState {
    // default: reject
    default void insertCoin(VendingMachine m, int paise) { m.refund(paise); }
    default void select(VendingMachine m, String code)   { m.display("Insert money first"); }
    default void cancel(VendingMachine m)                 { }
}

final class Idle implements VendingState {
    public void insertCoin(VendingMachine m, int paise) { m.addCredit(paise); m.setState(new HasCredit()); }
}

final class HasCredit implements VendingState {
    public void insertCoin(VendingMachine m, int paise) { m.addCredit(paise); }
    public void select(VendingMachine m, String code) {
        Product p = m.product(code);
        if (p == null || p.stock() == 0) { m.display("Unavailable"); return; }
        if (m.credit() < p.price())      { m.display("Insert " + (p.price() - m.credit()) + " more"); return; }
        m.setState(new Dispensing());
        // Dispensing → Idle when done, returns change
        m.dispense(p);
    }
    public void cancel(VendingMachine m) { m.refund(m.takeCredit()); m.setState(new Idle()); }
}

final class Dispensing implements VendingState {
    public void select(VendingMachine m, String code) { m.display("Please wait"); }
}
// VendingMachine delegates every button to `state` — no switch on an enum anywhere.
