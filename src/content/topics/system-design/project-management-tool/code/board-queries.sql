-- Column counts for a sprint board (index: tenant_id, sprint_id, status, rank)
SELECT status, count(*) FROM issues
WHERE tenant_id = $1 AND sprint_id = $2
GROUP BY status;

-- First page of a column, then keyset pagination by rank (never OFFSET)
SELECT id, key, title, assignee_id, rank FROM issues
WHERE tenant_id = $1 AND sprint_id = $2 AND status = 'IN_PROGRESS'
  AND rank > $3                     -- '' for the first page, then the last rank seen
ORDER BY rank
LIMIT 50;
