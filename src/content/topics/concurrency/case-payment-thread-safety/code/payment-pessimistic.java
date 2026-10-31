interface AccountRepository extends JpaRepository<Account, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)                    // SELECT … FOR UPDATE
    // consistent order → no deadlock
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAll(@Param("ids") Collection<String> ids);
}

@Service
class TransferService {
    @Transactional
    public Transfer transfer(String fromId, String toId, BigDecimal amount) {
        Map<String, Account> locked = repo.lockAll(List.of(fromId, toId)).stream()
            .collect(toMap(Account::getId, a -> a));
        Account from = locked.get(fromId), to = locked.get(toId);

        if (from.getBalance().compareTo(amount) < 0) throw new InsufficientFundsException(fromId, amount);
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // ledger row, same transaction
        Transfer t = transfers.save(Transfer.completed(fromId, toId, amount));
        // published after commit
        outbox.save(OutboxEvent.of("TransferCompleted", t));
        return t;
    }   // commit releases both row locks
}
