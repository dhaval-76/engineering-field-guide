-- Raw provider lines are stored as received; a unique key makes re-importing a file harmless.
CREATE TABLE settlement_lines (
  source     text   NOT NULL,          -- 'psp-a', 'bank-hdfc', …
  line_id    text   NOT NULL,          -- the provider's id for this line
  file_id    text   NOT NULL,          -- which file or API page it came from
  ref        text,                     -- our payment reference, if echoed back
  amount     bigint NOT NULL,          -- minor units; fees are separate lines
  currency   char(3) NOT NULL,
  value_date date   NOT NULL,
  PRIMARY KEY (source, line_id)
);

CREATE TABLE payments (
  id         text PRIMARY KEY,
  ref        text UNIQUE NOT NULL,
  amount     bigint NOT NULL,
  currency   char(3) NOT NULL,
  created_on date NOT NULL
);

-- One row per payment or line that needs attention; resolved by a person or a rule.
CREATE TABLE recon_breaks (
  run_date   date NOT NULL,
  kind       text NOT NULL,
  payment_id text,
  line_id    text,
  ours       bigint,
  theirs     bigint,
  status     text NOT NULL DEFAULT 'OPEN'
);
-- NULLs never conflict in a plain UNIQUE constraint, so a rerun would insert
-- one-sided breaks again; index on coalesce() (or UNIQUE NULLS NOT DISTINCT, PG 15+).
CREATE UNIQUE INDEX recon_breaks_once
  ON recon_breaks (run_date, kind, coalesce(payment_id, ''), coalesce(line_id, ''));

-- Pass 1 as one query: a full outer join on the reference classifies every row.
INSERT INTO recon_breaks (run_date, kind, payment_id, line_id, ours, theirs)
SELECT DATE '2026-10-15',
       CASE
         WHEN p.id IS NULL THEN 'MISSING_IN_LEDGER'
         WHEN s.line_id IS NULL THEN 'MISSING_AT_PROVIDER'
         ELSE 'AMOUNT_MISMATCH'
       END,
       p.id, s.line_id, p.amount, s.amount
FROM payments p
FULL OUTER JOIN settlement_lines s ON s.ref = p.ref
WHERE (p.id IS NULL OR s.line_id IS NULL
       OR p.amount <> s.amount OR p.currency <> s.currency)
  -- payments younger than the transit window are not breaks yet
  AND NOT (s.line_id IS NULL AND p.created_on > DATE '2026-10-15' - 3)
ON CONFLICT DO NOTHING;   -- rerunning the same day adds nothing new

-- Duplicates: one of our payments settled more than once.
SELECT ref, count(*) AS lines, sum(amount) AS settled
FROM settlement_lines
WHERE ref IS NOT NULL
GROUP BY ref
HAVING count(*) > 1;
