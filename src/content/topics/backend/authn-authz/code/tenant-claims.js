// Access token claims: {"sub":"u_1","org_id":"org_42","roles":["ADMIN"],"aud":"projects-api", …}
// After requireAuth verified the JWT, take the tenant from the token — never from the URL or body.
export function tenantContext(req, res, next) {
  req.tenant = { orgId: req.user.claims.org_id, roles: req.user.claims.roles ?? [] };
  if (!req.tenant.orgId) return res.status(403).end();
  next();
}

app.get('/projects', requireAuth, tenantContext, async (req, res) => {
  res.json(await projects.findByOrgId(req.tenant.orgId)); // every query scoped to the caller's org
});

app.post('/projects', requireAuth, tenantContext, async (req, res) => {
  if (!req.tenant.roles.some((r) => r === 'OWNER' || r === 'ADMIN')) return res.status(403).end();
  res.status(201).json(await projects.create({ ...req.body, orgId: req.tenant.orgId })); // body can't choose the org
});

// Defence in depth: PostgreSQL row-level security, set per transaction:
//   await client.query("SELECT set_config('app.org_id', $1, true)", [req.tenant.orgId]);
//   CREATE POLICY tenant_isolation ON projects USING (org_id = current_setting('app.org_id'));
