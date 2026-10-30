class DomainError(Exception):
    def __init__(self, status: int, code: str, message: str, **details):
        super().__init__(message)
        self.status, self.code, self.details = status, code, details

@app.exception_handler(DomainError)
async def domain_error(request: Request, exc: DomainError):
    return JSONResponse(status_code=exc.status, media_type="application/problem+json", content={
        "type": f"https://api.example.com/problems/{exc.code}",
        "title": str(exc), "status": exc.status, **exc.details,
    })

@app.exception_handler(Exception)
async def unexpected(request: Request, exc: Exception):
    logger.exception("unhandled error")
    return JSONResponse(status_code=500, media_type="application/problem+json",
                        content={"type": "https://api.example.com/problems/internal_error",
                                 "title": "Something went wrong", "status": 500})
