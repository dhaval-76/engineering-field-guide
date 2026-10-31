// ✗ High-level policy hard-wired to a low-level detail.
class NotificationService {
    // can't swap, can't test
    private final SmtpEmailSender sender = new SmtpEmailSender();
    void notify(User u, String msg) { sender.send(u.email(), msg); }
}

// ✓ Both depend on an abstraction owned by the high-level module.
interface MessageSender { void send(User to, String msg); }

class SmtpEmailSender implements MessageSender { public void send(User to, String msg) { /* SMTP */ } }
class SmsSender       implements MessageSender { public void send(User to, String msg) { /* SMS API */ } }

class NotificationService {
    private final MessageSender sender;
    NotificationService(MessageSender sender) { this.sender = sender; }   // injected
    void notify(User u, String msg) { sender.send(u, msg); }
}

// Wiring happens at the edge (main, or a DI container like Spring):
var service = new NotificationService(new SmsSender());
