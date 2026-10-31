// Step builder: each step returns an object exposing only the next legal call, so "url,
// then method, then (body only for POST/PUT), then build" is enforced. (TypeScript checks
// it at compile time.)
export function newRequest() {
  const req = { headers: {}, timeoutMs: 5000 };
  const buildStep = {
    header(k, v) { req.headers[k] = v; return buildStep; },
    timeout(ms) { req.timeoutMs = ms; return buildStep; },
    build: () => Object.freeze({ ...req, headers: Object.freeze({ ...req.headers }) }),
  };
  const bodyStep = { body(json) { req.body = json; return buildStep; } };
  const methodStep = {
    get() { req.method = 'GET'; return buildStep; },
    post() { req.method = 'POST'; return bodyStep; },
    put() { req.method = 'PUT'; return bodyStep; },
  };
  return { url(u) { req.url = u; return methodStep; } };
}

const r = newRequest().url('https://api.example.com/orders').post().body('{}').build();
// newRequest().url('…').get().body('{}')   ← TypeError: body is not a function (GET has
// no body step)
