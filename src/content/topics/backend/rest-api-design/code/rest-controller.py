# FastAPI: pydantic models give validation and a 422 response with field errors for free
class LineItem(BaseModel):
    sku: str = Field(min_length=1)
    quantity: PositiveInt

class CreateOrder(BaseModel):
    customer_id: str = Field(min_length=1)
    items: list[LineItem] = Field(min_length=1)
    note: str | None = Field(default=None, max_length=500)

@app.post("/v1/orders", status_code=201)
async def create_order(body: CreateOrder, response: Response,
                       idempotency_key: str = Header(alias="Idempotency-Key")):
    order = await orders.create(body, idempotency_key)
    response.headers["Location"] = f"/v1/orders/{order.id}"
    return order
