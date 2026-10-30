async function loadDashboard(userId) {
  const timeout = (ms) => AbortSignal.timeout(ms);
  const [user, recent, unread, balance] = await Promise.allSettled([
    users.get(userId, { signal: timeout(1000) }),
    orders.recent(userId, { signal: timeout(1000) }),
    notifications.unreadCount(userId, { signal: timeout(1000) }),
    accounts.balance(userId, { signal: timeout(1000) }),
  ]);
  if (user.status === 'rejected' || balance.status === 'rejected') throw new Error('dashboard unavailable');
  return {
    user: user.value,
    balance: balance.value,
    recent: recent.status === 'fulfilled' ? recent.value : [],
    unread: unread.status === 'fulfilled' ? unread.value : null,
  };
}
