// BEFORE: four independent calls in sequence → 4 × 750 ms = 3 s
Dashboard load(String userId) {
    return new Dashboard(users.get(userId), orders.recent(userId),
                         notifications.unreadCount(userId), accounts.balance(userId));
}

// AFTER: in parallel with a deadline and per-part fallbacks → ≈ 750 ms
Dashboard loadFast(String userId) {
    var user    = supplyAsync(() -> users.get(userId), io).orTimeout(1, SECONDS);
    var recent  = supplyAsync(() -> orders.recent(userId), io)
                      .completeOnTimeout(List.of(), 1, SECONDS);                 // optional panel
    var unread  = supplyAsync(() -> notifications.unreadCount(userId), io)
                      .exceptionally(e -> -1L);                                  // show "–" on failure
    var balance = supplyAsync(() -> accounts.balance(userId), io).orTimeout(1, SECONDS);

    return allOf(user, recent, unread, balance)
        .thenApply(v -> new Dashboard(user.join(), recent.join(), unread.join(), balance.join()))
        .join();
}
// Then ask why each call takes 750 ms: a missing index? no cache? a chatty downstream? — and fix that too.
