@Transactional
public void lifecycle() {
    // TRANSIENT: plain object, Hibernate doesn't know it
    Order order = new Order("C-42");
    // MANAGED: tracked; INSERT queued until flush
    em.persist(order);

    // still MANAGED: no SQL yet, just a field change
    order.setStatus(Status.PAID);
    Order same = em.find(Order.class, order.getId());
    assert same == order;                     // identity map: same instance, no SELECT

    // INSERT (and UPDATE) sent now; also happens at commit
    em.flush();
    // DETACHED: still in memory, no longer tracked
    em.detach(order);
    order.setStatus(Status.CANCELLED);        // ignored — nobody is watching this object

    Order managed = em.merge(order);          // copies its state onto a managed instance…
    // …and returns THAT one. Keep using `managed`.
    assert managed != order;

    em.remove(managed);                       // REMOVED: DELETE at flush
// commit: flush, then the persistence context closes and anything left becomes DETACHED
}
