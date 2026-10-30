import { createHash } from 'node:crypto';

const VIRTUAL_SHARDS = 1024;

// Stable across services and languages: first 8 bytes of SHA-256, as an unsigned integer.
export function bucketOf(tenantId) {
  const digest = createHash('sha256').update(String(tenantId)).digest();
  return Number(digest.readBigUInt64BE(0) % BigInt(VIRTUAL_SHARDS));
}

export function createRouter({ bucketToDatabase, pinnedTenants = new Map(), pools }) {
  return {
    poolFor(tenantId) {
      const db = pinnedTenants.get(tenantId) ?? bucketToDatabase.get(bucketOf(tenantId));
      return pools[db];                       // a pg.Pool per physical database
    },
  };
}

// const pool = router.poolFor(req.tenantId);
// await pool.query('SELECT … FROM orders WHERE tenant_id = $1 AND id = $2', [req.tenantId, id]);
