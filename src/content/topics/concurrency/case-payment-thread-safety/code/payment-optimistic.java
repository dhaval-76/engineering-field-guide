@Entity
class Account {
    @Id private String id;
    private BigDecimal balance;
    // UPDATE … SET balance=?, version=version+1 WHERE id=? AND version=?
    @Version private long version;
}

@Service
class WalletService {
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 20, random = true))
    @Transactional
    public void debit(String accountId, BigDecimal amount) {
        Account acc = repo.findById(accountId).orElseThrow();
        if (acc.getBalance().compareTo(amount) < 0) throw new InsufficientFundsException(accountId, amount);
        acc.setBalance(acc.getBalance().subtract(amount));
        // On commit Hibernate's UPDATE matches 0 rows if someone else changed the row
        // since we read it → optimistic-lock exception → the whole transaction is retried
        // with fresh data.
    }
}
