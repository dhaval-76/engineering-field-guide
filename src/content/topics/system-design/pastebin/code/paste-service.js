// Express + AWS SDK v3 (S3) + node-postgres
const MAX_BYTES = 10 * 1024 * 1024;

export async function createPaste(ownerId, content, ttlSeconds, visibility) {
  const bytes = Buffer.from(content, 'utf8');
  if (bytes.length > MAX_BYTES) throw Object.assign(new Error('too large'), { status: 413 });
  const id = scrambled(await ids.nextId());
  const expiresAt = ttlSeconds ? new Date(Date.now() + ttlSeconds * 1000) : null;
  await s3.send(
    new PutObjectCommand({
      Bucket: BUCKET,
      Key: `pastes/${id}`,
      Body: bytes,
      ContentType: 'text/plain; charset=utf-8',
    }),
  ); // content first…
  await pool.query(
    'INSERT INTO pastes (id, owner_id, size, visibility, expires_at) VALUES ($1, $2, $3, $4, $5)',
    // …then metadata (a crash leaves only an orphan object)
    [id, ownerId, bytes.length, visibility, expiresAt],
  );
  return { id, expiresAt };
}

app.get('/raw/:id', async (req, res) => {
  const p = await pastes.findById(req.params.id);
  if (!p) return res.status(404).end();
  const now = Date.now();
  if (p.expiresAt && p.expiresAt <= now) return res.status(410).end();
  if (p.visibility === 'PRIVATE' && p.ownerId !== req.user?.id) return res.status(404).end();
  // never beyond expiry
  const maxAge = Math.min(3600, p.expiresAt ? Math.floor((p.expiresAt - now) / 1000) : 3600);
  res.set(
    'Cache-Control',
    p.visibility === 'PRIVATE' ? 'private, no-store' : `public, max-age=${maxAge}`,
  );
  // immutable content: id is a perfect ETag
  res.set('ETag', `"${p.id}"`);
  const obj = await s3.send(new GetObjectCommand({ Bucket: BUCKET, Key: `pastes/${p.id}` }));
  res.type('text/plain; charset=utf-8').send(Buffer.from(await obj.Body.transformToByteArray()));
});
