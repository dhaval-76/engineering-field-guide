from sqlalchemy import inspect
from sqlalchemy.orm import Session

with Session(engine) as session, session.begin():
    order = Order(customer_id="C-42")      # transient
    session.add(order)                     # pending: INSERT queued until flush
    session.flush()                        # persistent: INSERT sent, order.id assigned

    order.status = "PAID"                  # tracked change, no SQL yet
    same = session.get(Order, order.id)    # identity map: no SELECT…
    assert same is order                   # …and the very same object

    session.expunge(order)                 # detached: no longer tracked
    managed = session.merge(order)         # like JPA: returns the tracked copy
    assert inspect(managed).persistent

    session.delete(managed)                # deleted at the next flush
# leaving `session.begin()` flushes and commits; leaving `Session` closes it
