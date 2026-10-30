@Transactional
public void lifecycle() {
    Order order = new Order("C-42");          // TRANSIENT: plain object, Hibernate doesn't know it
    em.persist(order);                        // MANAGED: tracked; INSERT queued until flush

    order.setStatus(Status.PAID);             // still MANAGED: no SQL yet, just a field change
    Order same = em.find(Order.class, order.getId());
    assert same == order;                     // identity map: same instance, no SELECT

    em.flush();                               // INSERT (and UPDATE) sent now; also happens at commit
    em.detach(order);                         // DETACHED: still in memory, no longer tracked
    order.setStatus(Status.CANCELLED);        // ignored — nobody is watching this object

    Order managed = em.merge(order);          // copies its state onto a managed instance…
    assert managed != order;                  // …and returns THAT one. Keep using `managed`.

    em.remove(managed);                       // REMOVED: DELETE at flush
}   // commit: flush, then the persistence context closes and anything left becomes DETACHED
