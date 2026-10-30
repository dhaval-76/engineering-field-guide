# FastAPI + PyJWT
from jwt import PyJWKClient, decode, InvalidTokenError

jwks = PyJWKClient("https://auth.example.com/.well-known/jwks.json")

def current_user(authorization: str = Header(...)) -> dict:
    token = authorization.removeprefix("Bearer ")
    try:
        key = jwks.get_signing_key_from_jwt(token).key
        return decode(token, key, algorithms=["RS256"], audience="orders-api",
                      issuer="https://auth.example.com", leeway=30)
    except InvalidTokenError:
        raise HTTPException(401, headers={"WWW-Authenticate": "Bearer"})

@app.get("/v1/orders/{order_id}")
def get_order(order_id: str, user: dict = Depends(current_user)):
    if "orders:read" not in user.get("scope", "").split():
        raise HTTPException(403)
    ...
