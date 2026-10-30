const CATEGORIES = new Set(['billing', 'bug', 'account', 'other']);

export async function classify(ticketText) {
  const body = {
    model: 'claude-sonnet-5-5',
    max_tokens: 200,
    temperature: 0,
    system:
      'You classify support tickets. Reply with JSON only: ' +
      '{"category": "billing|bug|account|other", "priority": "low|medium|high", "summary": "<= 20 words"}',
    messages: [{ role: 'user', content: `<ticket>\n${ticketText}\n</ticket>` }],
  };
  const text = await callWithRetry(body);
  const t = JSON.parse(text); // throws on invalid JSON
  if (!CATEGORIES.has(t.category)) throw new Error(`bad category ${t.category}`);
  return t;
}

async function callWithRetry(body, attempts = 4) {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'x-api-key': process.env.ANTHROPIC_API_KEY,
        'anthropic-version': '2023-06-01',
        'content-type': 'application/json',
      },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(30_000),
    });
    if (res.ok) {
      const data = await res.json();
      console.info('llm usage', data.usage.input_tokens, data.usage.output_tokens);
      return data.content[0].text;
    }
    const retryable = res.status === 429 || res.status >= 500;
    if (!retryable || attempt === attempts - 1) throw new Error(`LLM call failed: ${res.status}`);
    const retryAfter = Number(res.headers.get('retry-after'));
    const waitMs = retryAfter ? retryAfter * 1000 : 2 ** attempt * 500 + Math.random() * 250;
    await new Promise((r) => setTimeout(r, waitMs));
  }
}
