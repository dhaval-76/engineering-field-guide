// ✗ Three reasons to change: persistence rules, email provider, audit format.
class UserService {
    void register(User u) {
        db.save(u);
        smtp.send(u.email(), "Welcome!");
        auditFile.append("registered " + u.id());
    }
}

// ✓ Each collaborator owns one concern; UserService only coordinates.
class UserService {
    private final UserRepository users;
    private final WelcomeNotifier notifier;
    private final AuditLog audit;

    UserService(UserRepository users, WelcomeNotifier notifier, AuditLog audit) {
        this.users = users; this.notifier = notifier; this.audit = audit;
    }

    void register(User u) {
        users.save(u);
        notifier.welcome(u);
        audit.record("user.registered", u.id());
    }
}
