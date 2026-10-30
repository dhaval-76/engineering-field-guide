@Service
class MessageService {
    private final SequenceAllocator sequences;   // strongly consistent per-conversation counter
    private final MessageStore store;
    private final DedupStore dedup;              // client_msg_id → seq, with TTL
    private final KafkaTemplate<String, Delivered> kafka;

    /** Called by a gateway when a client sends. Returns the ack the sender's app shows as "sent". */
    public Ack accept(SendCommand cmd) {
        Long existing = dedup.get(cmd.senderId(), cmd.clientMsgId());
        if (existing != null) return new Ack(cmd.clientMsgId(), existing);   // retry → same answer

        long seq = sequences.next(cmd.conversationId());
        Message m = new Message(cmd.conversationId(), seq, UUID.randomUUID(), cmd.clientMsgId(),
                                cmd.senderId(), cmd.body(), Instant.now());
        store.append(m);                                  // durable BEFORE anyone is told
        dedup.put(cmd.senderId(), cmd.clientMsgId(), seq);
        kafka.send("messages", cmd.conversationId().toString(), Delivered.of(m));   // key keeps order per conversation
        return new Ack(cmd.clientMsgId(), seq);
    }
}

@Component
class FanOut {
    private final Members members;
    private final SessionRegistry sessions;      // user → [(deviceId, gatewayId)], TTL refreshed by heartbeats
    private final GatewayBus gateways;           // publish to a specific gateway's channel
    private final PushService push;

    @KafkaListener(topics = "messages", groupId = "fanout")
    void on(Delivered d) {
        for (UUID member : members.of(d.conversationId())) {
            if (member.equals(d.senderId())) continue;
            List<DeviceSession> online = sessions.devicesOf(member);
            online.forEach(s -> gateways.publish(s.gatewayId(), new Push(s.deviceId(), d)));
            if (online.isEmpty()) push.notifyLater(member, d.preview());   // batched, respects mute settings
        }
    }
}
// On reconnect the gateway calls store.after(conversationId, lastSeenSeq) for each conversation with activity.
