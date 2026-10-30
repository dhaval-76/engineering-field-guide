// Node 18+: fetch (undici) keeps connections alive per origin by default.
// Tune the pool explicitly for a busy service:
import { Agent, setGlobalDispatcher } from 'undici';

setGlobalDispatcher(new Agent({
  connections: 50,              // max sockets per origin
  keepAliveTimeout: 30_000,     // reuse idle sockets for 30 s
  connect: { timeout: 500 },    // TCP + TLS
}));

export async function fetchCart(sessionId) {
  const res = await fetch('https://shop.example.com/cart', {
    headers: { cookie: `session=${sessionId}` },
    signal: AbortSignal.timeout(2000),
  });
  return res.text();
}
