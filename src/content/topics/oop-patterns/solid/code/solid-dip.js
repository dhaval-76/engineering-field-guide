export class NotificationService {
  // Depends on "something with send(user, msg)", supplied from outside.
  constructor(sender) {
    this.sender = sender;
  }
  notify(user, msg) {
    return this.sender.send(user, msg);
  }
}

// Composition root (app startup) decides the concrete implementation:
// const service = new NotificationService(new SmsSender(twilioClient));
// In tests: new NotificationService({ send: async () => {} })
