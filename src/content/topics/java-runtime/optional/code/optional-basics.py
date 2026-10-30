from typing import Optional


def find_user(user_id: str) -> Optional["User"]:   # type hint documents possible absence
    return users.get(user_id)


user = find_user("42")
name = user.name if user is not None else "Guest"

# Chained lookups: getattr with defaults, or explicit checks
city = getattr(getattr(getattr(user, "profile", None), "address", None), "city", None) or "Unknown"
