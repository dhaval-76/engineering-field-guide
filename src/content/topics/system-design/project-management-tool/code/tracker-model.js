// node-postgres. Workflow-checked issue transition with optimistic locking, history and
// an outbox event.
export async function transition(
  c,
  { tenantId, issueId, expectedVersion, to, actor, fields = {}, newRank = null },
) {
  const issue = await issues.get(c, tenantId, issueId);
  const t = await workflows.transition(c, tenantId, issue.workflowId, issue.status, to);
  if (!t) throw httpError(422, `invalid transition ${issue.status} → ${to}`);
  if (t.requiredRole && !actor.hasProjectRole(issue.projectId, t.requiredRole))
    throw httpError(403, 'not allowed');
  for (const f of t.requiredFields)
    if (fields[f] == null) throw httpError(422, `missing field ${f}`);

  const { rowCount } = await c.query(
    `UPDATE issues SET status = $1, rank = coalesce($2, rank), version = version + 1, updated_at = now()
     WHERE tenant_id = $3 AND id = $4 AND version = $5`,
    [to, newRank, tenantId, issueId, expectedVersion],
  );
  // someone else changed it
  if (rowCount === 0) throw httpError(409, 'issue changed — reload');

  await c.query(
    `INSERT INTO issue_history (tenant_id, issue_id, actor_id, field, old_value, new_value)
                 VALUES ($1, $2, $3, 'status', $4, $5)`,
    [tenantId, issueId, actor.id, issue.status, to],
  );
  await outbox.publish(c, tenantId, 'IssueTransitioned', issueId, { from: issue.status, to });
}
// Call inside one transaction: BEGIN … transition(client, …) … COMMIT.
