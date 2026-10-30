public class SavingsAccount extends BankAccount {      // SavingsAccount IS-A BankAccount
    private final double annualRate;
    private int withdrawalsLeftThisMonth = 3;

    public SavingsAccount(String number, long openingPaise, double annualRate) {
        super(number, openingPaise);                     // reuse the parent's validation
        this.annualRate = annualRate;
    }

    public void applyMonthlyInterest() {
        deposit(Math.round(balance() * annualRate / 12)); // reuse parent behaviour
    }

    @Override
    public void withdraw(long paise) {
        if (withdrawalsLeftThisMonth == 0) throw new IllegalStateException("monthly limit reached");
        super.withdraw(paise);                           // parent still guards the balance
        withdrawalsLeftThisMonth--;
    }
}
