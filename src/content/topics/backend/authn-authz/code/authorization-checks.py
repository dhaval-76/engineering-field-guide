# FastAPI: authorization in dependencies and in the queries — never trust ids from the
# client alone.
from fastapi import Depends, HTTPException

def require_role(*roles: str):
    # current_user verifies the JWT
    def check(user: dict = Depends(current_user)) -> dict:
        if not set(roles) & set(user.get("roles", [])):
            raise HTTPException(403)
        return user
    return check

# Role check: who may call this at all
@app.delete("/orders/{order_id}", status_code=204)
def delete_order(order_id: str, user: dict = Depends(require_role("ADMIN"))):
    orders.delete_by_id(order_id)

# Ownership check: the data decides — compare the resource with the caller
@app.get("/orders/{order_id}")
def get_order(order_id: str, user: dict = Depends(current_user)):
    order = orders.view(order_id)
    if order is None or (order.customer_id != user["sub"] and "SUPPORT" not in user.get("roles", [])):
        # 404, not 403: don't reveal that someone else's order exists
        raise HTTPException(404)
    return order

# Better for lists: scope the QUERY to the caller (never find_all() then filter)
@app.get("/me/orders")
def my_orders(cursor: str | None = None, user: dict = Depends(current_user)):
    return orders.find_by_customer_id(user["sub"], cursor)
# Test all three: no token → 401; valid token without permission → 403/404; someone else's
# id → 404.
