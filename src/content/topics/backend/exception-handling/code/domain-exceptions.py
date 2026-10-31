# A small hierarchy of business errors, each carrying what the client needs to know.
class DomainError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code                         # stable, documented: "order_not_found"

class NotFoundError(DomainError):
    def __init__(self, resource: str, id: str):
        super().__init__(f"{resource}_not_found", f"{resource} {id} does not exist")

class ConflictError(DomainError):
    pass

class BusinessRuleError(DomainError):
    def __init__(self, code: str, message: str, details: dict | None = None):
        super().__init__(code, message)
        self.details = dict(details or {})

# Services raise meaningful errors and know nothing about HTTP:
# raise BusinessRuleError("insufficient_funds", "Balance is too low", {"account_id": aid,
# "shortfall": s})

# One FastAPI handler maps them to responses (RFC 9457 problem details):
from fastapi import Request
from fastapi.responses import JSONResponse

STATUS = {NotFoundError: 404, ConflictError: 409, BusinessRuleError: 422}

@app.exception_handler(DomainError)
async def domain_error(request: Request, err: DomainError) -> JSONResponse:
    status = next((s for t, s in STATUS.items() if isinstance(err, t)), 400)
    return JSONResponse(status_code=status, media_type="application/problem+json",
                        content={"type": f"https://errors.example.com/{err.code}", "title": str(err),
                                 "status": status, "code": err.code, **getattr(err, "details", {})})
