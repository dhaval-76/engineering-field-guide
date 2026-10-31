// Production code under test
export class UserService {
  constructor({ users, events }) {
    Object.assign(this, { users, events });
  }
  async deleteUser(userId) {
    const user = await this.users.findById(userId);
    if (!user) throw new UserNotFoundError(userId);
    await this.users.delete(user);
    await this.events.publish({ type: 'UserDeleted', userId });
  }
}
export class UserNotFoundError extends Error {
  constructor(id) { super(`user ${id} not found`); }
}

// user-service.test.js (Vitest)
import { it, expect, vi } from 'vitest';

it('deletes an existing user, then publishes the event', async () => {
  const calls = [];
  const users = { findById: vi.fn(async () => ({ id: 'U1' })), delete: vi.fn(async () => calls.push('delete')) };
  const events = { publish: vi.fn(async () => calls.push('publish')) };

  await new UserService({ users, events }).deleteUser('U1');

  expect(calls).toEqual(['delete', 'publish']);                            // the event only after the delete
  expect(events.publish).toHaveBeenCalledWith({ type: 'UserDeleted', userId: 'U1' });
});

it('throws for a missing user and changes nothing', async () => {
  const users = { findById: vi.fn(async () => null), delete: vi.fn() };
  const events = { publish: vi.fn() };

  await expect(new UserService({ users, events }).deleteUser('ghost')).rejects.toThrow(/ghost/);
  expect(users.delete).not.toHaveBeenCalled();
  expect(events.publish).not.toHaveBeenCalled();
});
