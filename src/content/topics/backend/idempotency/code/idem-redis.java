// Redis variant: fast, but the key and the business write live in different stores.
// Fine for deduplicating side effects that are themselves safe to repeat; for money, prefer the DB table.
Boolean first = redis.opsForValue()
    .setIfAbsent("idem:" + clientId + ":" + key, "IN_PROGRESS", Duration.ofSeconds(30));   // SET NX EX

if (Boolean.FALSE.equals(first)) {
    String stored = redis.opsForValue().get("idem:" + clientId + ":" + key);
    if ("IN_PROGRESS".equals(stored)) throw new RequestInProgress();     // 409 + Retry-After
    return objectMapper.readValue(stored, StoredResponse.class);         // replay
}
StoredResponse result = action.get();
redis.opsForValue().set("idem:" + clientId + ":" + key, objectMapper.writeValueAsString(result), Duration.ofHours(24));
return result;
