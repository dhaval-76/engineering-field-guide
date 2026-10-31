// Accept: idempotent per client message id, durable before acknowledging, ordered per conversation.
export async function accept(cmd) {
  const existing = await dedup.get(cmd.senderId, cmd.clientMsgId);
  if (existing !== null) return { clientMsgId: cmd.clientMsgId, seq: existing };   // retry → same answer

  const seq = await sequences.next(cmd.conversationId);       // strongly consistent per-conversation counter
  const m = { conversationId: cmd.conversationId, seq, id: crypto.randomUUID(), clientMsgId: cmd.clientMsgId,
              senderId: cmd.senderId, body: cmd.body, at: new Date() };
  await store.append(m);                                      // durable BEFORE anyone is told
  await dedup.put(cmd.senderId, cmd.clientMsgId, seq);
  await producer.send({ topic: 'messages', messages: [{ key: cmd.conversationId, value: JSON.stringify(m) }] }); // key keeps order
  return { clientMsgId: cmd.clientMsgId, seq };
}

// Fan-out consumer: deliver to online devices through their gateway, push to offline members.
export async function onDelivered(m) {
  for (const member of await members.of(m.conversationId)) {
    if (member === m.senderId) continue;
    const online = await sessions.devicesOf(member);           // [{ deviceId, gatewayId }], TTL refreshed by heartbeats
    for (const s of online) await gateways.publish(s.gatewayId, { deviceId: s.deviceId, message: m });
    if (online.length === 0) await push.notifyLater(member, preview(m)); // batched, respects mute settings
  }
}
// On reconnect the gateway calls store.after(conversationId, lastSeenSeq) for each conversation with activity.
