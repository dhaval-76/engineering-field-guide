// ✓ Coordinator + focused collaborators, passed in (easy to fake in tests).
export class UserService {
  constructor({ users, notifier, audit }) {
    Object.assign(this, { users, notifier, audit });
  }

  async register(user) {
    await this.users.save(user);
    await this.notifier.welcome(user);
    await this.audit.record('user.registered', user.id);
  }
}
