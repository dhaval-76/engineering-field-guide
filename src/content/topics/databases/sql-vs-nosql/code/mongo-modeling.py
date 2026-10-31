# PyMongo. An order is an aggregate: items and address embedded, the customer referenced.
# {"_id": "ord_8f2c", "customer_id": "cus_42", "status": "PLACED",
#  "items": [{"product_id": "prd_1", "name": "Air 14", "qty": 1, "unit_price": Decimal128("999.00")}, …],
#  "shipping": {"line1": "12 MG Road", "city": "Pune", "postcode": "411001"}, "created_at": datetime(…)}
# Reviews are NOT embedded in products: unbounded and written independently → their own collection.
from pymongo import ASCENDING, DESCENDING

orders = db["orders"]
orders.create_index([("customer_id", ASCENDING), ("created_at", DESCENDING)])   # equality, then sort

recent = list(orders.find({"customer_id": customer_id}).sort("created_at", DESCENDING).limit(20))   # uses the index
containing = list(orders.find({"items.product_id": product_id}))   # index items.product_id if this is hot

# Atomic in-place update of one document — no read-modify-write in application code
orders.update_one({"_id": order_id, "status": "PLACED"}, {"$set": {"status": "PAID"}})
