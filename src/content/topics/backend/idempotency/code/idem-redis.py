k = f"idem:{client_id}:{key}"
if not r.set(k, "IN_PROGRESS", nx=True, ex=30):
    stored = r.get(k)
    if stored == b"IN_PROGRESS":
        raise HTTPException(409, headers={"Retry-After": "1"})
    saved = json.loads(stored)
    return JSONResponse(saved["body"], status_code=saved["code"])
code, body = await action()
r.set(k, json.dumps({"code": code, "body": body}), ex=86_400)
return JSONResponse(body, status_code=code)
