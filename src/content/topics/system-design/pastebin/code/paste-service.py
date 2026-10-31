# FastAPI + boto3 (S3) + psycopg
from datetime import datetime, timedelta, timezone
from fastapi import HTTPException, Response

MAX_BYTES = 10 * 1024 * 1024

def create_paste(owner_id: str, content: str, ttl: timedelta | None, visibility: str) -> dict:
    data = content.encode()
    if len(data) > MAX_BYTES:
        raise HTTPException(413)
    paste_id = scrambled(ids.next_id())
    expires_at = datetime.now(timezone.utc) + ttl if ttl else None
    s3.put_object(Bucket=BUCKET, Key=f"pastes/{paste_id}", Body=data,
                  ContentType="text/plain; charset=utf-8")                      # content first…
    with pool.connection() as conn:
        conn.execute("INSERT INTO pastes (id, owner_id, size, visibility, expires_at) VALUES (%s, %s, %s, %s, %s)",
                     (paste_id, owner_id, len(data), visibility, expires_at))   # …then metadata
    return {"id": paste_id, "expires_at": expires_at}

@app.get("/raw/{paste_id}")
def raw(paste_id: str, user=Depends(optional_user)):
    p = pastes.find_by_id(paste_id)
    if p is None:
        raise HTTPException(404)
    now = datetime.now(timezone.utc)
    if p.expires_at and p.expires_at <= now:
        raise HTTPException(410)
    if p.visibility == "PRIVATE" and (user is None or user.id != p.owner_id):
        raise HTTPException(404)
    max_age = 3600 if not p.expires_at else min(3600, int((p.expires_at - now).total_seconds()))   # ≤ time left
    body = s3.get_object(Bucket=BUCKET, Key=f"pastes/{p.id}")["Body"].read()
    return Response(body, media_type="text/plain; charset=utf-8", headers={
        "Cache-Control": "private, no-store" if p.visibility == "PRIVATE" else f"public, max-age={max_age}",
        "ETag": f'"{p.id}"'})                                                   # immutable content: id is the ETag
