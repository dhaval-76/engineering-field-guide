# FastAPI + redis-py (asyncio) + aiokafka
from datetime import datetime, timezone
from fastapi import HTTPException, Path, Request
from fastapi.responses import RedirectResponse


@app.get("/{code}")
async def redirect(
    request: Request, code: str = Path(pattern=r"^([0-9a-zA-Z]{7}|[a-z0-9-]{3,32})$")
):
    target = await redis.get(f"link:{code}")
    if target is None:
        link = await links.find_by_code(code)
        if link is None:
            await redis.set(f"link:{code}", "", ex=60)  # negative cache
            raise HTTPException(404)
        if link.expires_at and link.expires_at <= datetime.now(timezone.utc):
            raise HTTPException(410)
        target = link.long_url
        await redis.set(f"link:{code}", target, ex=cache_ttl_seconds(link, 24 * 3600))
    elif target == b"" or target == "":
        raise HTTPException(404)
    # Fire and forget: don't make the redirect wait for analytics.
    await producer.send(
        "clicks",
        key=code.encode(),
        value={"code": code, "referer": request.headers.get("referer"), "ip": request.client.host},
    )
    return RedirectResponse(target if isinstance(target, str) else target.decode(), status_code=302)


async def create_link(url: str, alias: str | None, expires_at, owner_id: str):
    if not await safe_browsing.is_safe(url):
        raise HTTPException(400, "unsafe url")
    code = alias or scrambled(await ids.next_id())
    try:
        return await links.insert(
            code=code, url=url, owner_id=owner_id, expires_at=expires_at, custom=bool(alias)
        )
    # UNIQUE(code) settles alias races
    except UniqueViolation:
        raise HTTPException(409, "alias taken")
