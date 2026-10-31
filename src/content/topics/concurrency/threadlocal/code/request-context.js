// Node: AsyncLocalStorage follows a request through awaits and callbacks (there are no
// request threads).
import { AsyncLocalStorage } from 'node:async_hooks';

export const requestContext = new AsyncLocalStorage();

app.use((req, res, next) => {
  const ctx = { userId: req.get('x-user-id'), traceId: req.get('x-trace-id') };
  // everything started inside sees ctx; nothing to clean up
  requestContext.run(ctx, next);
});

// anywhere later, even after several awaits:
log.info(`[${requestContext.getStore()?.traceId}] refund issued`);
