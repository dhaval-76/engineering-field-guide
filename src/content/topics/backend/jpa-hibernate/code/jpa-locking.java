// 1. OPTIMISTIC — @Version on the entity. Hibernate writes:
//    UPDATE product SET stock = ?, version = 8 WHERE id = ? AND version = 7
//    0 rows → someone else committed first → OptimisticLockException.
//    Catch ObjectOptimisticLockingFailureException and either return 409 to the client
//    or retry the WHOLE transaction (a fresh read), never just the flush.
@Transactional
public void rename(long id, String name) {
    repo.findById(id).orElseThrow().setName(name);
}

// 2. PESSIMISTIC — SELECT … FOR UPDATE: a second buyer waits until the first commits.
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(long id);

    // 3. ATOMIC — the check and the write in one statement; no lock held across Java code.
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int reserve(long id, int qty);
}

@Transactional
public void buy(long productId, int qty) {
    if (repo.reserve(productId, qty) == 0) throw new OutOfStockException(productId);
}
