class UserService:
    """✓ A coordinator with focused collaborators, passed in (easy to fake in tests)."""

    def __init__(self, users, notifier, audit):
        self.users, self.notifier, self.audit = users, notifier, audit

    def register(self, user) -> None:
        self.users.save(user)                       # persistence is the repository's job
        self.notifier.welcome(user)                 # messaging is the notifier's job
        self.audit.record("user.registered", user.id)
