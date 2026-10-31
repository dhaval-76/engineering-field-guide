# Positional arguments: which one is the phone number? What do None and True mean?
User("Asha", None, None, "asha@example.com", 28, True, None, "ADMIN")

# Python's usual fix is keyword(-only) arguments with defaults — named and
# self-documenting:
class User:
    def __init__(self, name: str, *, email: str, age: int = 0, phone: str | None = None,
                 verified: bool = False, role: str = "MEMBER"):
        ...

User("Asha", email="asha@example.com", age=28, verified=True, role="ADMIN")
# A builder still helps when construction has steps or cross-field validation.
