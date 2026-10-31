# Access token claims: {"sub":"u_1","org_id":"org_42","roles":["ADMIN"],"aud":"projects-api", …}
from dataclasses import dataclass
from fastapi import Depends, HTTPException

@dataclass(frozen=True)
class Tenant:
    org_id: str
    roles: tuple[str, ...]

def tenant(claims: dict = Depends(current_user)) -> Tenant:
    """The tenant comes from the verified token — never from the URL or the request body."""
    if not claims.get("org_id"):
        raise HTTPException(403)
    return Tenant(claims["org_id"], tuple(claims.get("roles", ())))

@app.get("/projects")
def list_projects(t: Tenant = Depends(tenant)):
    return projects.find_by_org_id(t.org_id)                    # every query scoped to the caller's org

@app.post("/projects", status_code=201)
def create_project(req: NewProject, t: Tenant = Depends(tenant)):
    if not {"OWNER", "ADMIN"} & set(t.roles):
        raise HTTPException(403)
    return projects.create(org_id=t.org_id, **req.model_dump())  # body can't choose the org

# Defence in depth: PostgreSQL row-level security, set per transaction:
#   conn.execute(text("SELECT set_config('app.org_id', :org, true)"), {"org": t.org_id})
#   CREATE POLICY tenant_isolation ON projects USING (org_id = current_setting('app.org_id'));
