// BEFORE — SQL injection, three responsibilities, hard-wired dependencies, growing if-chain.
async function processUser(userId) {
  const user = await db.query(`SELECT * FROM users WHERE id=${userId}`);
  if (user.isAdmin) {
    await emailService.sendAdminReport(user.email);
    await db.query(`UPDATE users SET last_login=NOW() WHERE id=${userId}`);
  }
}

// AFTER
// users: { findById(id), touchLastLogin(id) } — DIP: an abstraction with parameterised queries inside
// hooks: [{ appliesTo(user), onLogin(user) }] — OCP: new behaviour = new hook
const adminReportHook = (mailer) => ({
  appliesTo: (u) => u.isAdmin,
  onLogin: (u) => mailer.sendAdminReport(u),
});

export class LoginService {
  constructor(users, hooks) {
    Object.assign(this, { users, hooks });
  }

  async recordLogin(userId) {                     // SRP: only orchestrates a login
    const user = await this.users.findById(userId);
    if (!user) throw new Error(`user ${userId} not found`);
    await this.users.touchLastLogin(userId);
    for (const h of this.hooks) if (h.appliesTo(user)) await h.onLogin(user);
  }
}
