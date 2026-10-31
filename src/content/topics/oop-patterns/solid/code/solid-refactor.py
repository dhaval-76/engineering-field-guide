# BEFORE — SQL injection, three responsibilities, hard-wired dependencies, growing if-chain.
def process_user(user_id):
    user = db.query(f"SELECT * FROM users WHERE id={user_id}")
    if user.is_admin:
        email_service.send_admin_report(user.email)
        db.execute(f"UPDATE users SET last_login=NOW() WHERE id={user_id}")

# AFTER
from typing import Protocol

class UserRepository(Protocol):               # DIP: an abstraction; parameterised queries live inside
    def find_by_id(self, user_id: str): ...
    def touch_last_login(self, user_id: str) -> None: ...

class LoginHook(Protocol):                    # OCP: new behaviour = new hook
    def applies_to(self, user) -> bool: ...
    def on_login(self, user) -> None: ...

class AdminReportHook:
    def __init__(self, mailer):
        self.mailer = mailer
    def applies_to(self, user) -> bool:
        return user.is_admin
    def on_login(self, user) -> None:
        self.mailer.send_admin_report(user)

class LoginService:
    def __init__(self, users: UserRepository, hooks: list[LoginHook]):
        self.users, self.hooks = users, hooks

    def record_login(self, user_id: str) -> None:   # SRP: only orchestrates a login
        user = self.users.find_by_id(user_id)
        if user is None:
            raise LookupError(f"user {user_id} not found")
        self.users.touch_last_login(user_id)
        for h in self.hooks:
            if h.applies_to(user):
                h.on_login(user)
