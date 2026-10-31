// Per-tenant token budget per minute, plus a fallback chain of providers. provider: {
// name, complete(prompt, { timeoutMs }) -> Promise<{ text, inputTokens, outputTokens }> }
export class LlmGateway {
  constructor(chain, tokensPerMinute) {
    this.chain = chain;
    this.tokensPerMinute = tokensPerMinute;
    this.usage = new Map(); // tenant -> { minute, tokens }
  }

  async complete(prompt, now = Date.now()) {
    const estimate = Math.floor(prompt.text.length / 4) + prompt.maxTokens;
    this.#reserve(prompt.tenant, estimate, now);
    const errors = [];
    for (const provider of this.chain) {
      try {
        const c = await provider.complete(prompt, { timeoutMs: 20_000 });
        this.#adjust(prompt.tenant, c.inputTokens + c.outputTokens - estimate, now);
        return { ...c, provider: provider.name };
      } catch (e) {
        errors.push(`${provider.name}: ${e.message}`);
      }
    }
    this.#adjust(prompt.tenant, -estimate, now);
    throw new Error(`all providers failed ${errors.join('; ')}`);
  }

  #reserve(tenant, tokens, now) {
    const minute = Math.floor(now / 60_000);
    const u = this.usage.get(tenant);
    const used = u && u.minute === minute ? u.tokens : 0;
    if (used + tokens > this.tokensPerMinute) throw new Error('429 token budget exceeded');
    this.usage.set(tenant, { minute, tokens: used + tokens });
  }

  #adjust(tenant, delta, now) {
    const u = this.usage.get(tenant);
    if (u && u.minute === Math.floor(now / 60_000)) u.tokens = Math.max(0, u.tokens + delta);
  }

  used(tenant) {
    return this.usage.get(tenant)?.tokens ?? 0;
  }
}
