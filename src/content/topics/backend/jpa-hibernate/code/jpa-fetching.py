from sqlalchemy import select
from sqlalchemy.orm import selectinload, joinedload

# ✗ N+1: relationship("OrderLine") is lazy="select" by default
orders = session.scalars(select(Order).where(Order.customer_id == customer_id)).all()
for o in orders:
    print(len(o.lines))          # one SELECT per order

# ✓ selectinload: a second query with WHERE order_id IN (...) — safe with LIMIT/OFFSET
orders = session.scalars(
    select(Order)
    .where(Order.customer_id == customer_id)
    # joined for many-to-one
    .options(selectinload(Order.lines), joinedload(Order.customer))
    .limit(20)
).all()

# ✓ Fail loudly instead of lazy loading: lazy="raise" on the relationship,
#   or .options(raiseload("*")) on a query
