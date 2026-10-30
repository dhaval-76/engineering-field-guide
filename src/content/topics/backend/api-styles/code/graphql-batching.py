import strawberry
from strawberry.dataloader import DataLoader
from strawberry.types import Info

@strawberry.type
class Customer:
    id: strawberry.ID
    name: str

@strawberry.type
class Order:
    id: strawberry.ID
    total_cents: int
    customer_id: strawberry.Private[str]

    @strawberry.field
    async def customer(self, info: Info) -> Customer:
        return await info.context["customer_loader"].load(self.customer_id)

async def get_context(request) -> dict:
    db = request.app.state.db

    async def load_customers(ids: list[str]) -> list[Customer | None]:
        rows = await db.customers_by_ids(ids)           # one query for the whole batch
        by_id = {c.id: c for c in rows}
        return [by_id.get(i) for i in ids]              # same order as ids

    # A new loader per request: batching within the request, no cross-user cache.
    return {"db": db, "customer_loader": DataLoader(load_fn=load_customers)}

@strawberry.type
class Query:
    @strawberry.field
    async def order(self, info: Info, id: strawberry.ID) -> Order | None:
        return await info.context["db"].order_by_id(id)

schema = strawberry.Schema(query=Query)
