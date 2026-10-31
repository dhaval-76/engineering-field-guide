// Accept: idempotent per client message id, durable before acknowledging, ordered per
// conversation.
export async function accept(cmd) {
  const existing = await dedup.get(cmd.senderId, cmd.clientMsgId);
  // retry → same answer
  if (existing !== null) return { clientMsgId: cmd.clientMsgId, seq: existing };

  // strongly consistent per-conversation counter
  const seq = await sequences.next(cmd.conversationId);
  const m = {
    conversationId: cmd.conversationId,
    seq,
    id: crypto.randomUUID(),
    clientMsgId: cmd.clientMsgId,
    senderId: cmd.senderId,
    body: cmd.body,
    at: new Date(),
  };
  // durable BEFORE anyone is told
  await store.append(m);
  await dedup.put(cmd.senderId, cmd.clientMsgId, seq);
  // key keeps order
  await producer.send({
    topic: 'messages',
    messages: [{ key: cmd.conversationId, value: JSON.stringify(m) }],
  });
  return { clientMsgId: cmd.clientMsgId, seq };
}

// Fan-out consumer: deliver to online devices through their gateway, push to offline
// members.
export async function onDelivered(m) {
  for (const member of await members.of(m.conversationId)) {
    if (member === m.senderId) continue;
    // [{ deviceId, gatewayId }], TTL refreshed by heartbeats
    const online = await sessions.devicesOf(member);
    for (const s of online)
      await gateways.publish(s.gatewayId, { deviceId: s.deviceId, message: m });
    // batched, respects mute settings
    if (online.length === 0) await push.notifyLater(member, preview(m));
  }
}
// On reconnect the gateway calls store.after(conversationId, lastSeenSeq) for each
// conversation with activity.
