// Express + ioredis + kafkajs
app.get(/^\/([0-9a-zA-Z]{7}|[a-z0-9-]{3,32})$/, async (req, res) => {
  const code = req.params[0];
  let target = await redis.get(`link:${code}`);
  if (target === null) {
    const link = await links.findByCode(code);
    if (!link) {
      await redis.set(`link:${code}`, '', 'EX', 60);                 // negative cache
      return res.status(404).end();
    }
    if (link.expiresAt && link.expiresAt <= new Date()) return res.status(410).end();
    target = link.longUrl;
    await redis.set(`link:${code}`, target, 'EX', cacheTtlSeconds(link, 24 * 3600));
  } else if (target === '') {
    return res.status(404).end();
  }
  // Fire and forget: don't make the redirect wait for analytics.
  producer.send({ topic: 'clicks', messages: [{ key: code, value: JSON.stringify({
    code, at: Date.now(), referer: req.get('referer'), ua: req.get('user-agent'), ip: req.ip }) }] })
    .catch((e) => log.warn({ err: e }, 'click not recorded'));
  res.redirect(302, target);
});

export async function createLink(url, alias, expiresAt, ownerId) {
  if (!(await safeBrowsing.isSafe(url))) throw Object.assign(new Error('unsafe url'), { status: 400 });
  const code = alias ?? scrambled(await ids.nextId());
  try {
    return await links.insert({ code, url, ownerId, expiresAt, custom: Boolean(alias) });
  } catch (e) {
    if (e.code === '23505') throw Object.assign(new Error('alias taken'), { status: 409 }); // UNIQUE(code) settles races
    throw e;
  }
}
