@Service
class IssueTransitions {
    private final WorkflowRepository workflows;
    private final JdbcTemplate jdbc;

    @Transactional
    public void transition(long tenantId, long issueId, int expectedVersion, String to, Actor actor,
                           Map<String, Object> fields, String newRank) {
        Issue issue = issues.get(tenantId, issueId);
        Transition t = workflows.transition(tenantId, issue.workflowId(), issue.status(), to)
            .orElseThrow(() -> new InvalidTransitionException(issue.status(), to));
        if (t.requiredRole() != null && !actor.hasProjectRole(issue.projectId(), t.requiredRole()))
            throw new ForbiddenException("transition " + issue.status() + " → " + to);
        t.requiredFields().forEach(f -> { if (fields.get(f) == null) throw new MissingFieldException(f); });

        int updated = jdbc.update("""
            UPDATE issues SET status = ?, rank = coalesce(?, rank), version = version + 1, updated_at = now()
            WHERE tenant_id = ? AND id = ? AND version = ?""",
            to, newRank, tenantId, issueId, expectedVersion);
        // someone else changed it → 409
        if (updated == 0) throw new StaleIssueException(issueId);

        jdbc.update("INSERT INTO issue_history (tenant_id, issue_id, actor_id, field, old_value, new_value) VALUES (?,?,?,?,?,?)",
                    tenantId, issueId, actor.id(), "status", issue.status(), to);
        outbox.publish(tenantId, "IssueTransitioned", issueId, Map.of("from", issue.status(), "to", to));
    }
}
