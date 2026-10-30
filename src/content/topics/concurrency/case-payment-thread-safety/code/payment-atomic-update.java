@Repository
class AccountDao {
    private final JdbcTemplate jdbc;
    AccountDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Check and debit in ONE statement: the database does it atomically under a row lock. */
    @Transactional
    public boolean debit(String accountId, BigDecimal amount) {
        int rows = jdbc.update(
            "UPDATE accounts SET balance = balance - ? WHERE id = ? AND balance >= ?",
            amount, accountId, amount);
        return rows == 1;               // 0 rows → insufficient funds (or unknown account)
    }
}
