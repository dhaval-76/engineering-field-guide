from typing import Awaitable, Callable

# In Python, ISP shows up as "accept the smallest thing you need".
# ✗ Requires a whole repository object just to read one user.
async def greeting(user_repo, user_id):
    u = await user_repo.find_by_id(user_id)
    return f"Hi {u.name}"

# ✓ Ask only for the capability you use — trivially fakeable in tests.
async def greeting_for(find_user: Callable[[int], Awaitable], user_id: int) -> str:
    u = await find_user(user_id)
    return f"Hi {u.name}"

# await greeting_for(repo.find_by_id, 42)
# await greeting_for(fake_find_user, 1)      # a test double is just a function
