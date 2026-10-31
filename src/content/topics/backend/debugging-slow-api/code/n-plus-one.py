# SQLAlchemy 2.x
from sqlalchemy import func, select
from sqlalchemy.orm import selectinload, joinedload

# ✗ N+1: one query for orders, then one lazy load per order → 501 queries for 500 orders
orders = session.scalars(select(Order).where(Order.customer_id == customer_id)).all()
total = sum(len(o.items) for o in orders)

# ✓ selectinload: one extra query with WHERE order_id IN (…) — works with pagination
orders = session.scalars(select(Order).where(Order.customer_id == customer_id)
                         .options(selectinload(Order.items))).all()

# ✓ joinedload: a single JOIN — good for many-to-one, can multiply rows for collections
# ✓ Often best for read endpoints: an aggregate with exactly the columns the response needs
summaries = session.execute(
    select(Order.id, Order.created_at, func.count(Item.id), func.sum(Item.price * Item.quantity))
    .join(Order.items).where(Order.customer_id == customer_id).group_by(Order.id, Order.created_at)).all()

# Make lazy loads fail loudly in development: relationship(..., lazy="raise")
# Catch it in tests: count statements with an event listener ("before_cursor_execute") and assert the number.
