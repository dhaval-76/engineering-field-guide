interface LlmProvider {
    String name();
    Completion complete(Prompt p, Duration timeout) throws Exception;
}
record Prompt(String tenant, String text, int maxTokens) {}
record Completion(String text, int inputTokens, int outputTokens, String provider) {}

/** Per-tenant token budget per minute, plus a fallback chain of providers. */
class LlmGateway {
    private final List<LlmProvider> chain;                  // primary first
    private final long tokensPerMinute;
    private final Map<String, long[]> usage = new ConcurrentHashMap<>();   // tenant -> {minute, tokens}

    LlmGateway(List<LlmProvider> chain, long tokensPerMinute) {
        this.chain = chain; this.tokensPerMinute = tokensPerMinute;
    }

    Completion complete(Prompt p, long nowMillis) {
        long estimate = p.text().length() / 4 + p.maxTokens();             // rough worst case
        reserve(p.tenant(), estimate, nowMillis);
        List<String> errors = new ArrayList<>();
        for (LlmProvider provider : chain) {
            try {
                Completion c = provider.complete(p, Duration.ofSeconds(20));
                adjust(p.tenant(), c.inputTokens() + c.outputTokens() - estimate, nowMillis);
                return c;
            } catch (Exception e) {
                errors.add(provider.name() + ": " + e.getMessage());  // try the next provider
            }
        }
        adjust(p.tenant(), -estimate, nowMillis);                          // nothing was spent
        throw new IllegalStateException("all providers failed " + errors);
    }

    private void reserve(String tenant, long tokens, long now) {
        long minute = now / 60_000;
        usage.compute(tenant, (t, u) -> {
            long[] cur = (u == null || u[0] != minute) ? new long[] {minute, 0} : u;
            if (cur[1] + tokens > tokensPerMinute) throw new IllegalStateException("429 token budget exceeded");
            return new long[] {minute, cur[1] + tokens};
        });
    }

    private void adjust(String tenant, long delta, long now) {
        long minute = now / 60_000;
        usage.computeIfPresent(tenant, (t, u) -> u[0] == minute ? new long[] {minute, Math.max(0, u[1] + delta)} : u);
    }

    long used(String tenant) { long[] u = usage.get(tenant); return u == null ? 0 : u[1]; }
}
