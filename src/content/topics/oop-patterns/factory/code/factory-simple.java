interface Notifier { void send(String to, String message); }

final class EmailNotifier implements Notifier { public void send(String to, String m) { /* SMTP */ } }
final class SmsNotifier   implements Notifier { public void send(String to, String m) { /* SMS API */ } }
final class PushNotifier  implements Notifier { public void send(String to, String m) { /* FCM/APNs */ } }

// Simple factory: one place knows the concrete classes.
final class Notifiers {
    private Notifiers() {}

    static Notifier forChannel(Channel channel) {
        return switch (channel) {           // exhaustive over an enum: the compiler flags a missing case
            case EMAIL -> new EmailNotifier();
            case SMS   -> new SmsNotifier();
            case PUSH  -> new PushNotifier();
        };
    }
}

class AlertService {
    void alert(Channel channel, String to, String msg) {
        Notifiers.forChannel(channel).send(to, msg);   // no `new EmailNotifier()` here
    }
}
