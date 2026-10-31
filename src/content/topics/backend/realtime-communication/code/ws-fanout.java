// Spring WebSocket + Redis pub/sub: any instance can deliver to any room.
@Component
class RoomSocketHandler extends TextWebSocketHandler implements MessageListener {
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    RoomSocketHandler(StringRedisTemplate redis, RedisMessageListenerContainer container) {
        this.redis = redis;
        container.addMessageListener(this, new PatternTopic("room:*"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode m = json.readTree(message.getPayload());
        String room = "room:" + m.get("room").asText();
        switch (m.get("type").asText()) {
            case "join" -> rooms.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet())
                    // send timeout + buffer limit
                    .add(new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024));
            // publish to every instance
            case "say" -> redis.convertAndSend(room, message.getPayload());
        }
    }

    @Override   // a message published by any instance
    public void onMessage(Message message, byte[] pattern) {
        String room = new String(message.getChannel(), StandardCharsets.UTF_8);
        TextMessage out = new TextMessage(message.getBody());
        for (WebSocketSession s : rooms.getOrDefault(room, Set.of())) {
            // slow/broken client
            try { s.sendMessage(out); } catch (Exception e) { closeQuietly(s); }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        rooms.values().forEach(set -> set.removeIf(s -> s.getId().equals(session.getId())));
    }
}
