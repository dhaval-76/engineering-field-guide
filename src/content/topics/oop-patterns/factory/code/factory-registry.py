# A registry that plug-ins add themselves to — here with a decorator.
_processors: dict[str, object] = {}

def processor(method: str):
    def register(cls):
        if method in _processors:
            raise ValueError(f"duplicate processor for {method}")
        _processors[method] = cls()
        return cls
    return register

def processor_for(method: str):
    try:
        return _processors[method]
    except KeyError:
        raise ValueError(f"unsupported payment method {method}") from None

# card.py
@processor("CARD")
class CardProcessor:
    def pay(self, payment) -> dict:
        return {"ok": True}
