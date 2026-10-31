from dataclasses import dataclass, field

@dataclass(frozen=True)
class HttpRequest:
    url: str
    method: str
    body: str | None
    headers: dict
    timeout_s: float

class _BuildStep:
    def __init__(self, req: dict):
        self._req = req
    def header(self, k: str, v: str) -> "_BuildStep":
        self._req["headers"][k] = v
        return self
    def timeout(self, seconds: float) -> "_BuildStep":
        self._req["timeout_s"] = seconds
        return self
    def build(self) -> HttpRequest:
        return HttpRequest(**{**self._req, "headers": dict(self._req["headers"])})

class _BodyStep:
    def __init__(self, req: dict):
        self._req = req
    def body(self, json: str) -> _BuildStep:
        self._req["body"] = json
        return _BuildStep(self._req)

class _MethodStep:
    def __init__(self, req: dict):
        self._req = req
    def get(self) -> _BuildStep:
        self._req["method"] = "GET"
        return _BuildStep(self._req)
    def post(self) -> _BodyStep:
        self._req["method"] = "POST"
        return _BodyStep(self._req)
    def put(self) -> _BodyStep:
        self._req["method"] = "PUT"
        return _BodyStep(self._req)

def new_request():
    """Each step exposes only the next legal call; type checkers (mypy) flag misuse before runtime."""
    class _UrlStep:
        def url(self, u: str) -> _MethodStep:
            return _MethodStep({"url": u, "method": None, "body": None, "headers": {}, "timeout_s": 5.0})
    return _UrlStep()

r = new_request().url("https://api.example.com/orders").post().body("{}").build()
# new_request().url("…").get().body("{}")   ← AttributeError: GET has no body step
