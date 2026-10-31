// BEFORE — SQL injection, three responsibilities, hard-wired dependencies, growing
// if-chain.
class UserService {
    public void processUser(String userId) {
        User user = db.query("SELECT * FROM users WHERE id=" + userId);
        if (user.isAdmin()) {
            emailService.sendAdminReport(user.email());
            db.execute("UPDATE users SET last_login=NOW() WHERE id=" + userId);
        }
    }
}

// AFTER
interface UserRepository { Optional<User> findById(String id); void touchLastLogin(String id); }
// new behaviour = new hook
interface LoginHook { boolean appliesTo(User u); void onLogin(User u); }

class AdminReportHook implements LoginHook {
    private final ReportMailer mailer;
    AdminReportHook(ReportMailer mailer) { this.mailer = mailer; }
    public boolean appliesTo(User u) { return u.isAdmin(); }
    public void onLogin(User u) { mailer.sendAdminReport(u); }
}

class LoginService {
    // DIP: abstraction, parameterised queries inside
    private final UserRepository users;
    private final List<LoginHook> hooks;          // OCP: extend by adding hooks

    LoginService(UserRepository users, List<LoginHook> hooks) { this.users = users; this.hooks = hooks; }

    public void recordLogin(String userId) {      // SRP: only orchestrates a login
        User user = users.findById(userId).orElseThrow(() -> new UserNotFound(userId));
        users.touchLastLogin(userId);
        hooks.stream().filter(h -> h.appliesTo(user)).forEach(h -> h.onLogin(user));
    }
}
