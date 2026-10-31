# psycopg 3. Workflow-checked issue transition with optimistic locking, history and an outbox event.
class InvalidTransition(Exception): pass
class Forbidden(Exception): pass
class StaleIssue(Exception): pass          # → 409: someone else changed it

def transition(conn, tenant_id, issue_id, expected_version, to, actor, fields=None, new_rank=None):
    fields = fields or {}
    with conn.transaction():
        issue = issues.get(conn, tenant_id, issue_id)
        t = workflows.transition(conn, tenant_id, issue.workflow_id, issue.status, to)
        if t is None:
            raise InvalidTransition(f"{issue.status} → {to}")
        if t.required_role and not actor.has_project_role(issue.project_id, t.required_role):
            raise Forbidden(f"transition {issue.status} → {to}")
        missing = [f for f in t.required_fields if fields.get(f) is None]
        if missing:
            raise InvalidTransition(f"missing fields {missing}")
        updated = conn.execute(
            """UPDATE issues SET status = %s, rank = coalesce(%s, rank), version = version + 1, updated_at = now()
               WHERE tenant_id = %s AND id = %s AND version = %s""",
            (to, new_rank, tenant_id, issue_id, expected_version)).rowcount
        if updated == 0:
            raise StaleIssue(issue_id)
        conn.execute("""INSERT INTO issue_history (tenant_id, issue_id, actor_id, field, old_value, new_value)
                        VALUES (%s, %s, %s, 'status', %s, %s)""", (tenant_id, issue_id, actor.id, issue.status, to))
        outbox.publish(conn, tenant_id, "IssueTransitioned", issue_id, {"from": issue.status, "to": to})
