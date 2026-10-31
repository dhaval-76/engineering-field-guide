import uuid
from datetime import datetime, timezone

async def accept(cmd) -> dict:
    """Idempotent per client message id, durable before acknowledging, ordered per conversation."""
    existing = await dedup.get(cmd.sender_id, cmd.client_msg_id)
    if existing is not None:
        return {"client_msg_id": cmd.client_msg_id, "seq": existing}      # retry → same answer
    seq = await sequences.next(cmd.conversation_id)                       # per-conversation counter
    m = {"conversation_id": cmd.conversation_id, "seq": seq, "id": str(uuid.uuid4()),
         "client_msg_id": cmd.client_msg_id, "sender_id": cmd.sender_id, "body": cmd.body,
         "at": datetime.now(timezone.utc).isoformat()}
    await store.append(m)                                                 # durable BEFORE anyone is told
    await dedup.put(cmd.sender_id, cmd.client_msg_id, seq)
    await producer.send("messages", key=cmd.conversation_id.encode(), value=m)   # key keeps order
    return {"client_msg_id": cmd.client_msg_id, "seq": seq}

async def on_delivered(m: dict) -> None:
    """Fan-out: deliver to online devices through their gateway, push to offline members."""
    for member in await members.of(m["conversation_id"]):
        if member == m["sender_id"]:
            continue
        online = await sessions.devices_of(member)          # [(device_id, gateway_id)], TTL via heartbeats
        for device_id, gateway_id in online:
            await gateways.publish(gateway_id, {"device_id": device_id, "message": m})
        if not online:
            await push.notify_later(member, preview(m))      # batched, respects mute settings
# On reconnect the gateway calls store.after(conversation_id, last_seen_seq) for each active conversation.
