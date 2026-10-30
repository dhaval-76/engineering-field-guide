from dataclasses import dataclass


# Keyword arguments + defaults solve the readability problem directly.
@dataclass(frozen=True, kw_only=True)
class User:
    name: str
    email: str
    age: int = 0
    phone: str | None = None
    role: str = "USER"
    verified: bool = False

    def __post_init__(self):
        if not self.name.strip():
            raise ValueError("name is required")
        if "@" not in self.email:
            raise ValueError("valid email is required")


u = User(name="Asha", email="asha@example.com", role="ADMIN", verified=True)
