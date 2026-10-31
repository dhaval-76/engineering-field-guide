// AsyncLocalStorage: per-request context that follows the request across every await
// (Node's ThreadLocal).
import { AsyncLocalStorage } from 'node:async_hooks';

export const tenantContext = new AsyncLocalStorage();
export const currentTenant = () => tenantContext.getStore()?.tenantId;

export function tenantMiddleware(req, res, next) {
  // Take the tenant from the authenticated token's claims, never from a header the client
  // controls.
  const tenantId = req.user?.claims?.org_id;
  if (!tenantId) return res.status(403).end();
  // everything this request does sees the same store, then it's gone
  tenantContext.run({ tenantId }, next);
}

// Logger: add the tenant to every line (pino)
export const logger = pino({ mixin: () => ({ tenant: currentTenant() }) });
